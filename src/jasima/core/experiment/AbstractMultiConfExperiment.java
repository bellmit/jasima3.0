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
package jasima.core.experiment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jasima.core.util.MsgCategory;
import jasima.core.util.TypeUtil;
import jasima.core.util.Util;

/**
 * <p>
 * Base class for experiments that execute variations of a
 * {@code baseExperiment} by changing its properties.
 * </p>
 * <p>
 * The order in which properties are applied is determined by the length of the
 * property name to guarantee that sub-properties are set after the object
 * containing them. This also applies when {@link ComplexFactorSetter} is used.
 * Properties with equally long names are executed in an undefined order.
 * Exceptions are null, which is regarded as having a length of -1, and
 * {@link #KEY_EXPERIMENT}, which is regarded has having a length of -2.
 * {@link #KEY_EXPERIMENT} can be present in any number of configurations. If it
 * is present in all configurations, baseExperiment need not be set.
 * </p>
 * 
 * @author Robin Kreis
 * @author Torsten Hildebrandt
 */
public abstract class AbstractMultiConfExperiment extends AbstractMultiExperiment {

	private static final long serialVersionUID = 8651960788951812186L;

	public static final String KEY_EXPERIMENT = "@";

	/**
	 * Allows finer control of the way a base experiment is configured than the
	 * usual mechanism using JavaBean properties. If an object implementing
	 * ComplexFactorSetter is passed as a value when calling
	 * {@link FullFactorialExperiment#addFactor(String, Object)
	 * addFactor(String, Object)} then instead of setting a bean property the
	 * method {@link #configureExperiment(Experiment)} is called.
	 * 
	 * @see FullFactorialExperiment#addFactor(String, Object)
	 */
	@FunctionalInterface
	public interface ComplexFactorSetter extends Serializable {
		/**
		 * Configures an experiment.
		 * 
		 * @param e
		 *            The experiment to configure.
		 */
		void configureExperiment(final Experiment e);
	}

	/**
	 * An (optional) way to veto certain configurations, because some
	 * combinations of factors and their values might not make sense.
	 * 
	 * @see FullFactorialExperiment#setConfigurationValidator(ConfigurationValidator)
	 */
	public interface ConfigurationValidator extends Serializable {
		boolean isValid(Map<String, Object> configuration);
	}

	// parameters
	private Experiment baseExperiment = null;
	private ConfigurationValidator configurationValidator = null;

	// fields used during run
	protected int numConfs = 0;

	@Override
	public void init() {
		numConfs = 0;

		if (getBaseExperiment() == null) {
			throw new IllegalArgumentException("Please set a base experiment.");
		}

		super.init();
	}

	protected abstract void createExperiments();

	protected void handleConfig(Map<String, Object> conf) {
		if (!isValidConfiguration(conf))
			return;

		numConfs++;
		try {
			Experiment exp = createExperimentForConf(conf);
			experiments.add(exp);
		} catch (final Exception e) {
			String msg = e.getMessage();
			print(MsgCategory.ERROR, msg == null ? e.toString() : msg);
			print(MsgCategory.DEBUG, "%s", new Object() {
				@Override
				public String toString() {
					// lazy conversion to String only when message is
					// actually printed
					return Util.exceptionToString(e);
				}
			});

			experiments.add(new Experiment() {

				private static final long serialVersionUID = 4259612422796656502L;

				@Override
				protected void produceResults() {
					aborted++;
					super.produceResults();

					resultMap.put(Experiment.EXCEPTION_MESSAGE, e.getMessage());
					resultMap.put(Experiment.EXCEPTION, Util.exceptionToString(e));
				}

				@Override
				protected void performRun() {
					// do nothing
				}
			});
		}
	}

	protected Experiment createExperimentForConf(Map<String, Object> conf) {
		Experiment e = conf.containsKey(KEY_EXPERIMENT) ? ((Experiment) conf.get(KEY_EXPERIMENT)).silentClone()
				: getBaseExperiment().silentClone();
		configureRunExperiment(e);

		List<Map.Entry<String, Object>> entries = new ArrayList<Map.Entry<String, Object>>(conf.entrySet());
		// sort by length
		Collections.sort(entries, new Comparator<Map.Entry<String, Object>>() {
			@Override
			public int compare(Entry<String, Object> o1, Entry<String, Object> o2) {
				String a = o1.getKey();
				String b = o2.getKey();
				if (a == b)
					return 0;
				if (a == null)
					return -1;
				if (b == null)
					return 1;
				return a.length() - b.length();
			}
		});

		for (Map.Entry<String, Object> p : entries) {
			if (p.getKey().equals(KEY_EXPERIMENT))
				continue;
			if (p.getValue() != null && p.getValue() instanceof ComplexFactorSetter) {
				((ComplexFactorSetter) p.getValue()).configureExperiment(e);
			} else {
				TypeUtil.setPropertyValue(e, p.getKey(), TypeUtil.cloneIfPossible(p.getValue()));
			}
		}

		return e;
	}

	protected boolean isValidConfiguration(Map<String, Object> conf) {
		if (getConfigurationValidator() == null)
			return true;

		return getConfigurationValidator().isValid(conf);
	}

	@Override
	protected final String prefix() {
		return "conf";
	}

	/**
	 * Returns the number of experiment configurations to be executed.
	 */
	@Override
	public int getNumExperiments() {
		return numConfs;
	}

	public Experiment getBaseExperiment() {
		return baseExperiment;
	}

	/**
	 * Sets the base experiment that is executed multiple times in various
	 * configurations. Before experiment execution, a copy (clone) of
	 * {@code baseExperiment} is created and run. Therefore the specific
	 * experiment instance passed as the {@code baseExperiment} is never
	 * actually executed.
	 * 
	 * @param baseExperiment
	 *            The base experiment to use.
	 */
	public void setBaseExperiment(Experiment baseExperiment) {
		this.baseExperiment = baseExperiment;
	}

	public ConfigurationValidator getConfigurationValidator() {
		return configurationValidator;
	}

	/**
	 * Sets a {@link ConfigurationValidator}, which is used to veto certain
	 * impossible factor combinations.
	 * 
	 * @param configurationValidator
	 *            Sets the validator.
	 */
	public void setConfigurationValidator(ConfigurationValidator configurationValidator) {
		this.configurationValidator = configurationValidator;
	}

	@Override
	public AbstractMultiConfExperiment clone() throws CloneNotSupportedException {
		AbstractMultiConfExperiment e = (AbstractMultiConfExperiment) super.clone();

		if (baseExperiment != null)
			e.baseExperiment = baseExperiment.clone();

		return e;
	}
}
