package gsn.storage;

import gsn.Helpers;
import gsn.beans.DataType;
import gsn.beans.decorators.QueueDataNodeDecorator;
import gsn.beans.decorators.WrapperDecorator;
import gsn.beans.model.*;
import gsn.wrappers2.SystemTimeWrapper2;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

@Test
public class TestVirtualSensorModel {

    @BeforeClass
    public void setup() {
//        Helpers.initLoggerToDebug();
    }

    private WindowModel createCountBasedWindowModel() {

        ParameterModel parameterModel = new ParameterModel();
        parameterModel.setName("size");
        parameterModel.setDataType(DataType.NUMERIC);
        parameterModel.setDefaultValue("1");

        WindowModel windowModel = new WindowModel();
        windowModel.setName("CountbasedWindow");
        windowModel.setClassName("gsn.windows.CountBasedWindow");
        ArrayList<ParameterModel> models = new ArrayList<ParameterModel>();
        models.add(parameterModel);
        windowModel.setParameters(models);

        return windowModel;
    }

    private SlidingModel createCountBasedSlidingModel() {

        ParameterModel parameterModel = new ParameterModel();
        parameterModel.setName("size");
        parameterModel.setDataType(DataType.NUMERIC);
        parameterModel.setDefaultValue("1");

        SlidingModel slidingModel = new SlidingModel();
        slidingModel.setName("CountbasedSlide");
        slidingModel.setClassName("gsn.sliding.CountBasedSliding");
        ArrayList<ParameterModel> models = new ArrayList<ParameterModel>();
        models.add(parameterModel);
        slidingModel.setParameters(models);

        return slidingModel;
    }

    private WindowModel createTimeBasedWindowModel() {

        ParameterModel parameterModel = new ParameterModel();
        parameterModel.setName("size");
        parameterModel.setDataType(DataType.NUMERIC);
        parameterModel.setDefaultValue("1000");

        WindowModel windowModel = new WindowModel();
        windowModel.setName("TimebasedWindow");
        windowModel.setClassName("gsn.windows.TimeBasedWindow");
        ArrayList<ParameterModel> models = new ArrayList<ParameterModel>();
        models.add(parameterModel);
        windowModel.setParameters(models);

        return windowModel;
    }

    private SlidingModel createTimeBasedSlidingModel() {

        ParameterModel parameterModel = new ParameterModel();
        parameterModel.setName("size");
        parameterModel.setDataType(DataType.NUMERIC);
        parameterModel.setDefaultValue("1000");

        SlidingModel slidingModel = new SlidingModel();
        slidingModel.setName("TimebasedSlide");
        slidingModel.setClassName("gsn.sliding.TimeBasedSliding");
        ArrayList<ParameterModel> models = new ArrayList<ParameterModel>();
        models.add(parameterModel);
        slidingModel.setParameters(models);

        return slidingModel;
    }

    private WrapperModel createSampleWrapperModel() {

        ArrayList<ParameterModel> models = new ArrayList<ParameterModel>();
        ParameterModel parameterModel = new ParameterModel();
        parameterModel.setName("mode");
        parameterModel.setDataType(DataType.STRING);
        parameterModel.setDefaultValue("manual");
        models.add(parameterModel);

        parameterModel = new ParameterModel();
        parameterModel.setName("period");
        parameterModel.setDataType(DataType.NUMERIC);
        parameterModel.setDefaultValue("1000");
        models.add(parameterModel);

        WrapperModel wrapperModel = new WrapperModel();
        wrapperModel.setName("system-time-2");
        wrapperModel.setClassName("gsn.wrappers2.SystemTimeWrapper2");
        wrapperModel.setParameters(models);

        return wrapperModel;
    }

    private DataChannel createDataChannel(Window window, Sliding sliding, DataNode producer, DataNode consumer) {
        DataChannel dataChannel = new DataChannel();
        dataChannel.setWindow(window);
        dataChannel.setSliding(sliding);
        dataChannel.setProducer(producer);
        dataChannel.setConsumer(consumer);

        return dataChannel;
    }

    private Window createSampleWindow(WindowModel model, int size) {
        Window window = new Window();

        Parameter parameter = new Parameter();
        parameter.setModel(model.getParameters().get(0));
        parameter.setValue(String.valueOf(size));

        window.setModel(model);
        ArrayList<Parameter> parameters = new ArrayList<Parameter>();
        parameters.add(parameter);
        window.setParameters(parameters);

        return window;
    }

    private Sliding createSampleSliding(SlidingModel model, int size) {
        Sliding sliding = new Sliding();

        Parameter parameter = new Parameter();
        parameter.setModel(model.getParameters().get(0));
        parameter.setValue(String.valueOf(size));

        sliding.setModel(model);
        ArrayList<Parameter> parameters = new ArrayList<Parameter>();
        parameters.add(parameter);
        sliding.setParameters(parameters);

        return sliding;
    }

    private WrapperNode createSampleWrapperNode(WrapperModel model, String type, int timePeriod) {
        WrapperNode wrapperNode = new WrapperNode();
        wrapperNode.setName("test-wrapper-node");

        ArrayList<Parameter> parameters = new ArrayList<Parameter>();
        Parameter parameter = new Parameter();
        parameter.setModel(model.getParameters().get(0));
        parameter.setValue(type);
        parameters.add(parameter);

        parameter = new Parameter();
        parameter.setModel(model.getParameters().get(1));
        parameter.setValue(String.valueOf(timePeriod));
        parameters.add(parameter);

        wrapperNode.setModel(model);
        wrapperNode.setParameters(parameters);

        return wrapperNode;
    }

    private QueryNode createSampleQueryNode(String name, String query) {
        QueryNode queryNode = new QueryNode();
        queryNode.setQuery(query);
        queryNode.setName(name);
        return queryNode;
    }

    @Test
    public void testCreateSampleWindowModel() {
        Session session = HibernateUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();
        WindowModel windowModel = createCountBasedWindowModel();
        session.save(windowModel);
        tx.commit();
    }

    @Test
    public void testCreateSampleSlidingModel() {
        Session session = HibernateUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();
        SlidingModel slidingModel = createCountBasedSlidingModel();
        session.save(slidingModel);
        tx.commit();
    }

    @Test
    public void testCreateSampleWindow() {
        Session session = HibernateUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();
        WindowModel windowModel = createCountBasedWindowModel();
        session.save(windowModel);

        Window window = createSampleWindow(windowModel, 2);
        session.save(window);

        tx.commit();
    }

    @Test
    public void testCreateSampleSliding() {
        Session session = HibernateUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();
        SlidingModel slidingModel = createCountBasedSlidingModel();
        session.save(slidingModel);

        Sliding sliding = createSampleSliding(slidingModel, 2);
        session.save(sliding);

        tx.commit();
    }

    @Test
    public void testCreateSampleDataChannel() {
        Session session = HibernateUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();

        WrapperModel wrapperModel = createSampleWrapperModel();
        session.save(wrapperModel);


        SlidingModel slidingModel = createCountBasedSlidingModel();
        session.save(slidingModel);
        Sliding sliding = createSampleSliding(slidingModel, 2);
        session.save(sliding);

        WindowModel windowModel = createCountBasedWindowModel();
        session.save(windowModel);
        Window window = createSampleWindow(windowModel, 2);
        session.save(window);

        WrapperNode wrapperNode = createSampleWrapperNode(wrapperModel, "manual", 1000);
        session.save(wrapperNode);

        DataChannel dataChannel = createDataChannel(window, sliding, wrapperNode, wrapperNode);
        session.save(dataChannel);
        tx.commit();
    }

    @Test
    public void testCreateSampleWrapperModel() {
        Session session = HibernateUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();
        WrapperModel wrapperModel = createSampleWrapperModel();
        session.save(wrapperModel);
        tx.commit();
    }


    @Test
    public void testCreateSampleWrapper() {
        Session session = HibernateUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();
        WrapperModel wrapperModel = createSampleWrapperModel();
        session.save(wrapperModel);

        WrapperNode wrapperNode = createSampleWrapperNode(wrapperModel, "manual", 1000);
        session.save(wrapperNode);

        tx.commit();

        WrapperDecorator wrapperDeco = new WrapperDecorator(new QueueDataNodeDecorator(wrapperNode));
        wrapperDeco.initialize();
        SystemTimeWrapper2 wrapper = (SystemTimeWrapper2) wrapperDeco.getWrapper();
        wrapper.produceNext();
        wrapper.produceNext();
        wrapper.produceNext();
        wrapper.produceNext();
        wrapper.produceNext();
        wrapper.produceNext();
        wrapper.produceNext();
    }

    @Test
    public void testCreateSampleDataNodeGraph() {
        Session session = HibernateUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();

        WrapperModel wrapperModel = createSampleWrapperModel();
        session.save(wrapperModel);


        SlidingModel slidingModel = createCountBasedSlidingModel();
        session.save(slidingModel);

        Sliding sliding1 = createSampleSliding(slidingModel, 1);
        session.save(sliding1);
        Sliding sliding2 = createSampleSliding(slidingModel, 4);
        session.save(sliding2);
        Sliding sliding3 = createSampleSliding(slidingModel, 6);
        session.save(sliding3);
        Sliding sliding4 = createSampleSliding(slidingModel, 2);
        session.save(sliding4);
        Sliding sliding5 = createSampleSliding(slidingModel, 4);
        session.save(sliding5);
        Sliding sliding6 = createSampleSliding(slidingModel, 5);
        session.save(sliding6);
        Sliding sliding7 = createSampleSliding(slidingModel, 3);
        session.save(sliding7);


        WindowModel windowModel = createCountBasedWindowModel();
        session.save(windowModel);

        Window window1 = createSampleWindow(windowModel, 1);
        session.save(window1);
        Window window2 = createSampleWindow(windowModel, 3);
        session.save(window2);
        Window window3 = createSampleWindow(windowModel, 5);
        session.save(window3);
        Window window4 = createSampleWindow(windowModel, 2);
        session.save(window4);
        Window window5 = createSampleWindow(windowModel, 4);
        session.save(window5);
        Window window6 = createSampleWindow(windowModel, 6);
        session.save(window6);
        Window window7 = createSampleWindow(windowModel, 3);
        session.save(window7);


        WrapperNode wrapperNode1 = createSampleWrapperNode(wrapperModel, "manual", 1000);
        session.save(wrapperNode1);
        WrapperNode wrapperNode2 = createSampleWrapperNode(wrapperModel, "manual", 2000);
        session.save(wrapperNode2);
        WrapperNode wrapperNode3 = createSampleWrapperNode(wrapperModel, "manual", 3000);
        session.save(wrapperNode3);

        QueryNode queryNode1 = createSampleQueryNode("QN1", "Q1");
        session.save(queryNode1);
        QueryNode queryNode2 = createSampleQueryNode("QN2","Q2");
        session.save(queryNode2);
        QueryNode queryNode3 = createSampleQueryNode("QN3","Q3");
        session.save(queryNode3);
        QueryNode queryNode4 = createSampleQueryNode("QN4","Q4");
        session.save(queryNode4);

        DataChannel channel;
        channel = createDataChannel(window1, sliding1, wrapperNode1, queryNode1);
        session.save(channel);
        channel = createDataChannel(window2, sliding2, wrapperNode2, queryNode1);
        session.save(channel);
        channel = createDataChannel(window3, sliding3, queryNode1, queryNode2);
        session.save(channel);
        channel = createDataChannel(window4, sliding4, queryNode1, queryNode3);
        session.save(channel);
        channel = createDataChannel(window5, sliding5, wrapperNode3, queryNode3);
        session.save(channel);
        channel = createDataChannel(window6, sliding6, queryNode2, queryNode4);
        session.save(channel);
        channel = createDataChannel(window7, sliding7, queryNode3, queryNode4);
        session.save(channel);

        session.flush();

        Query query = session.createQuery("from QueryNode as qn left join fetch qn.inChannels where qn.name = ?");
        query.setString(0, "QN1");

        QueryNode queryNode = (QueryNode) query.uniqueResult();
        assertNotNull(queryNode.getInChannels());
        assertEquals(queryNode.getInChannels().size(), 2);

         tx.commit();
    }

    @Test
    public void testHibernateUtil() {
        Session session = HibernateUtil.getCurrentSession();

        Transaction tx = session.beginTransaction();

        tx.commit();
    }

}
