/*******************************************************************************
 * Copyright 2011, 2012 Torsten Hildebrandt and BIBA - Bremer Institut für Produktion und Logistik GmbH
 *
 * This file is part of jasima, v1.0.
 *
 * jasima is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jasima is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jasima.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ HolthausSimpleTest.class, JobShopTests.class,
		TestStaticInsts.class, TestOrderIndependence.class, TestSetups.class,
		TestBatching.class, TestBestOfFamilyBatching.class,
		TestGECCOContinuity.class, TestWinterSim2010Continuity.class,
		TestMIMAC.class, TestForAllResults.class, TestMimacFab4Trace.class,
		jasima.core.experiment.AllTests.class, TestDetailedTraces.class, TestDowntimes.class })
public class AllTests {

}
