package ch.epfl.gsn.metadata.web.services;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.query.Query;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QueryBuilderTest{

    private QueryBuilder subject = new QueryBuilder();

    @Mock
    private SensorQuery sensorQuery;

    @Test
    public void propertyNamesTest() throws Exception {
        when(sensorQuery.getObservedProperties()).thenReturn(Sets.newHashSet("snow", "tree"));

        Query query = subject.build(sensorQuery);
        System.out.println("query = " + query);
    }

    @Test
    public void boundingBoxTest() throws Exception {
        when(sensorQuery.hasValidBoundingBox()).thenReturn(true);
        when(sensorQuery.getBoundingBox()).thenReturn(new Box(new Point(46, 8.8), new Point(47, 9)));

        Query query = subject.build(sensorQuery);
        System.out.println("query = " + query);
    }

}