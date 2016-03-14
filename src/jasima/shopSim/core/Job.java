/*******************************************************************************
 * This file is part of jasima, v1.3, the Java simulator for manufacturing and 
 * logistics.
 *  
 * Copyright (c) 2015 		jasima solutions UG
 * Copyright (c) 2010-2015 Torsten Hildebrandt and jasima contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package jasima.shopSim.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import jasima.core.util.ValueStore;

/**
 * Main work unit in a shop.
 * 
 * @author Torsten Hildebrandt
 */
// TODO: PrioRuleTarget should be an interface
public class Job extends PrioRuleTarget implements Cloneable, ValueStore {

	/** Base class for workstation events. */
	public static class JobEvent {
	}

	// constants for events thrown by a job

	public static final JobEvent JOB_RELEASED = new JobEvent();
	public static final JobEvent JOB_FINISHED = new JobEvent();
	public static final JobEvent JOB_ARRIVED_IN_QUEUE = new JobEvent();
	public static final JobEvent JOB_REMOVED_FROM_QUEUE = new JobEvent();
	public static final JobEvent JOB_START_OPERATION = new JobEvent();
	public static final JobEvent JOB_END_OPERATION = new JobEvent();

	private final JobShop shop;

	private double arriveTime; // arrival time at current machine
	private WorkStation currMachine;
	// when will job finish processing on its current machine (if started)
	private double startTime;
	private double finishTime;
	private double relDate;
	private double dueDate;
	private int jobNum; // global number of job in system
	private int jobType;
	private double weight = 1.0d;
	private int taskNumber; // current operation
	private Operation[] ops;
	private double[] opDueDates;
	private boolean isFuture = false;
	private String name = null;
	private Route route = null;
	// we cache the value returned by remainingProcTime()
	private double remProcTime = -1.0d;

	private Job future;

	public Job(JobShop shop) {
		super();
		this.shop = shop;
	}

	public void setArriveTime(double fl) {
		arriveTime = fl;
	}

	public double getArriveTime() {
		return arriveTime;
	}

	public void setJobType(int job) {
		jobType = job;
	}

	public int getJobType() {
		return jobType;
	}

	public void setTaskNumber(int tn) {
		remProcTime = -1.0d;
		taskNumber = tn;
	}

	public int getTaskNumber() {
		return taskNumber;
	}

	public void setCurrMachine(WorkStation currMachine) {
		this.currMachine = currMachine;
	}

	public WorkStation getCurrMachine() {
		return currMachine;
	}

	public Operation getCurrentOperation() {
		return ops[taskNumber];
	}

	public double currProcTime() {
		return ops[taskNumber].getProcTime();
	}

	public double procSum() {
		double res = 0d;
		for (Operation o : ops) {
			res += o.getProcTime();
		}
		return res;
	}

	public double remainingProcTime() {
		if (remProcTime < 0.0d) {
			remProcTime = 0f;
			Operation[] ops = this.ops;
			for (int i = taskNumber; i < ops.length; i++) {
				remProcTime += ops[i].getProcTime();
			}
		}
		return remProcTime;
	}

	public int numOps() {
		return getOps().length;
	}

	public int numOpsLeft() {
		return getOps().length - getTaskNumber();
	}

	/**
	 * If the this job has one or more tasks yet to be done, send the job to the
	 * next machine on its route
	 */
	void proceed() {
		if (!isLastOperation()) {
			setTaskNumber(getTaskNumber() + 1);

			WorkStation mNext = ops[taskNumber].getMachine();
			mNext.enqueueOrProcess(this);
		} else {
			shop.jobFinished(this);
		}
	}

	void jobReleased() {
		shop.getSim().publishNotification(this, JOB_RELEASED);
	}

	void jobFinished() {
		shop.getSim().publishNotification(this, JOB_FINISHED);
	}

	void arriveInQueue(WorkStation workStation, double arrivesAt) {
		setCurrMachine(workStation);
		setArriveTime(arrivesAt);

		shop.getSim().publishNotification(this, JOB_ARRIVED_IN_QUEUE);
	}

	void removedFromQueue() {
		shop.getSim().publishNotification(this, JOB_REMOVED_FROM_QUEUE);
	}

	void startProcessing() {
		setFinishTime(currMachine.currMachine.procFinished);
		setStartTime(currMachine.shop().simTime());
		notifyNextMachine();

		shop.getSim().publishNotification(this, JOB_START_OPERATION);
	}

	void endProcessing() {
		shop.getSim().publishNotification(this, JOB_END_OPERATION);
	}

	/**
	 * Notify next machine of future arrival. This mehod is called whenever an
	 * operation is started. This method assumes isFinished to be set to the
	 * correct value before this method is called.
	 */
	public void notifyNextMachine() {
		if (!isLastOperation() && shop.isEnableLookAhead()) {
			final Job f = getMyFuture();
			final WorkStation next = f.ops[f.taskNumber].getMachine();
			next.futureArrival(f, getFinishTime());
		}
	}

	/**
	 * Returns a clone of this Job switched to the next operation.
	 * 
	 * @return The future clone of this job.
	 */
	public Job getMyFuture() {
		if (future == null) {
			future = silentClone();
			future.setFuture(true);
		}
		future.setTaskNumber(taskNumber + 1);
		return future;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Job clone() throws CloneNotSupportedException {
		Job j = (Job) super.clone();
		j.future = null;

		if (valueStore != null) {
			j.valueStore = (HashMap<Object, Object>) valueStore.clone();
		}

		return j;
	}

	public Job silentClone() {
		try {
			return clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	void setFuture(boolean isFuture) {
		this.isFuture = isFuture;
	}

	public boolean isFuture() {
		return isFuture;
	}

	public boolean isLastOperation() {
		return taskNumber == ops.length - 1;
	}

	@Override
	public String toString() {
		String s = getName() + "#" + taskNumber;
		if (isFuture())
			s += "(future)";
		return s;
	}

	public JobShop getShop() {
		return shop;
	}

	public double getDueDate() {
		return dueDate;
	}

	public void setDueDate(double dueDate) {
		this.dueDate = dueDate;
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public Job getFuture() {
		return future;
	}

	public void setFuture(Job future) {
		this.future = future;
	}

	public void setRelDate(double relDate) {
		this.relDate = relDate;
	}

	public double getRelDate() {
		return relDate;
	}

	public void setJobNum(int jobNum) {
		this.jobNum = jobNum;
	}

	@Override
	public int getJobNum() {
		return jobNum;
	}

	/**
	 * Sets the completion time of the current operation. This is called by a
	 * machine whenever processing starts.
	 * 
	 * @param finishTime
	 *            The finish time of the current {@link Operation}.
	 */
	public void setFinishTime(double finishTime) {
		this.finishTime = finishTime;
	}

	public double getFinishTime() {
		return finishTime;
	}

	public double getStartTime() {
		return startTime;
	}

	/**
	 * Sets the start time of the current operation. This is used internally and
	 * called by a machine whenever processing starts.
	 * 
	 * @param startTime
	 *            The start time of the current operation.
	 */
	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}

	public void setOpDueDates(double[] opDueDates) {
		this.opDueDates = opDueDates;
	}

	public double[] getOpDueDates() {
		return opDueDates;
	}

	@Override
	public double getCurrentOperationDueDate() {
		if (opDueDates == null) {
			setOpDueDates(computeDueDatesTWC(this, (dueDate - relDate) / procSum()));
		}

		return opDueDates[taskNumber];
	}

	public void setOps(Operation[] ops) {
		this.ops = ops;
		this.opDueDates = null;
	}

	public Operation[] getOps() {
		return ops;
	}

	@Override
	public Job job(int i) {
		if (i != 0)
			throw new IllegalArgumentException("" + i);
		return this;
	}

	@Override
	public int numJobsInBatch() {
		return 1;
	}

	/**
	 * Computes operational due dates based on the total work content method, /*
	 * i.e., proportional to an operation's processing time.
	 * 
	 * @param j
	 *            The job for which to compute operation due dates.
	 * @param ff
	 *            The due date factor to use.
	 * @return An array containing operation due dates for each operation of
	 *         {@code j}.
	 */
	public static double[] computeDueDatesTWC(Job j, double ff) {
		Operation[] ops = j.ops;

		double[] res = new double[ops.length];

		double due = j.getRelDate();

		for (int i = 0; i < res.length; i++) {
			due += ff * ops[i].getProcTime();
			res[i] = due;
		}

		return res;
	}

	@Override
	public boolean isBatch() {
		return false;
	}

	@Override
	public String getName() {
		if (name == null)
			return getClass().getSimpleName() + "." + jobType + "." + getJobNum();
		else
			return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return The route this object is following (might be null).
	 */
	public Route getRoute() {
		return route;
	}

	/**
	 * Sets the {@link Route} this object is following. This might be
	 * {@code null}, as not every job has to be created using a {@code Route}
	 * (e.g. in a dynamic job shop where each {@code Job} has its unique rule).
	 * 
	 * @param route
	 *            The route this Job is following.
	 */
	public void setRoute(Route route) {
		this.route = route;
	}

	//
	//
	// ValueStore implementation
	//
	//

	private HashMap<Object, Object> valueStore;

	/**
	 * Offers a simple get/put-mechanism to store and retrieve information as a
	 * kind of global data store. This can be used as a simple extension
	 * mechanism.
	 * 
	 * @param key
	 *            The key name.
	 * @param value
	 *            value to assign to {@code key}.
	 * @see #valueStoreGet(Object)
	 */
	@Override
	public void valueStorePut(Object key, Object value) {
		if (valueStore == null)
			valueStore = new HashMap<Object, Object>();
		valueStore.put(key, value);
	}

	/**
	 * Retrieves a value from the value store.
	 * 
	 * @param key
	 *            The entry to return, e.g., identified by a name.
	 * @return The value associated with {@code key}.
	 * @see #valueStorePut(Object, Object)
	 */
	@Override
	public Object valueStoreGet(Object key) {
		if (valueStore == null)
			return null;
		else
			return valueStore.get(key);
	}

	/**
	 * Returns the number of keys in this job's value store.
	 */
	@Override
	public int valueStoreGetNumKeys() {
		return (valueStore == null) ? 0 : valueStore.size();
	}

	/**
	 * Returns a list of all keys contained in this job's value store.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<Object> valueStoreGetAllKeys() {
		if (valueStore == null)
			return Collections.EMPTY_SET;
		else
			return valueStore.keySet();
	}

	/**
	 * Removes an entry from this job's value store.
	 * 
	 * @return The value previously associated with "key", or null, if no such
	 *         key was found.
	 */
	@Override
	public Object valueStoreRemove(Object key) {
		if (valueStore == null)
			return null;
		else
			return valueStore.remove(key);
	}

}
