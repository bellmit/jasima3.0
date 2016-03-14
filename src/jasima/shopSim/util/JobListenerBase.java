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
package jasima.shopSim.util;

import static jasima.shopSim.core.Job.JOB_ARRIVED_IN_QUEUE;
import static jasima.shopSim.core.Job.JOB_END_OPERATION;
import static jasima.shopSim.core.Job.JOB_FINISHED;
import static jasima.shopSim.core.Job.JOB_RELEASED;
import static jasima.shopSim.core.Job.JOB_REMOVED_FROM_QUEUE;
import static jasima.shopSim.core.Job.JOB_START_OPERATION;

import jasima.core.util.observer.NotifierService;
import jasima.core.util.observer.Subscriber;
import jasima.shopSim.core.Job;
import jasima.shopSim.core.Job.JobEvent;
import jasima.shopSim.core.JobShop;
import jasima.shopSim.core.WorkStation;

/**
 * This class can be used as a base class for classes collecting results based
 * on job releases/job completions.
 * 
 * @author Torsten Hildebrandt
 */
public abstract class JobListenerBase implements Subscriber, Cloneable {

	public JobListenerBase() {
		super();
	}

	@Override
	public void register(NotifierService ns) {
		ns.addSubscription(JobEvent.class, this);
	}

	@Override
	public final void inform(Object o, Object e) {
		Job j = (Job) o;
		JobEvent event = (JobEvent) e;

		final JobShop shop = j.getShop();
		if (event == JOB_RELEASED) {
			released(shop, j);
		} else if (event == JOB_FINISHED) {
			finished(shop, j);
		} else if (event == JOB_ARRIVED_IN_QUEUE) {
			arrivedInQueue(shop, j);
		} else if (event == JOB_REMOVED_FROM_QUEUE) {
			removedFromQueue(shop, j);
		} else if (event == JOB_START_OPERATION) {
			WorkStation m = j.getCurrMachine();
			operationStarted(shop, j, m.oldSetupState, m.newSetupState, m.setupTime);
		} else if (event == JOB_END_OPERATION) {
			endOperation(shop, j);
		} else {
			handleOther(shop, j, event);
		}
	}

	protected void handleOther(JobShop shop, Job j, JobEvent event) {
	}

	protected void endOperation(JobShop shop, Job j) {
	}

	protected void operationStarted(JobShop shop, Job j, int oldSetupState, int newSetupState, double setupTime) {
	}

	protected void removedFromQueue(JobShop shop, Job j) {
	}

	protected void arrivedInQueue(JobShop shop, Job j) {
	}

	protected void finished(JobShop shop, Job j) {
	}

	protected void released(JobShop shop, Job j) {
	}

	@Override
	public JobListenerBase clone() throws CloneNotSupportedException {
		return (JobListenerBase) super.clone();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}
