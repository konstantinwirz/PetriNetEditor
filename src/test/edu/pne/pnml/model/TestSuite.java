package edu.pne.pnml.model;


import org.junit.runner.RunWith;
import org.junit.runners.Suite;


/**
 * Created by konstantin on 23/11/14.
 */

@RunWith(Suite.class)
@Suite.SuiteClasses(value = {PositionTest.class, ArcTest.class,TransitionTest.class, PlaceTest.class, NetTest.class})
public class TestSuite {
}
