package gsn.storage;

import gsn.beans.DataType;
import gsn.beans.model.*;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.Test;

import java.util.ArrayList;

public class TestVirtualSensorModel {

    private WindowModel createSampleWindowModel() {

        ParameterModel parameterModel = new ParameterModel();
        parameterModel.setName("size");
        parameterModel.setDataType(DataType.NUMERIC);
        parameterModel.setDefaultValue("1");

        WindowModel windowModel = new WindowModel();
        windowModel.setName("CountbasedWindow");
        windowModel.setClassName("gsn.beans.DataWindow");
        ArrayList<ParameterModel> models = new ArrayList<ParameterModel>();
        models.add(parameterModel);
        windowModel.setParameters(models);

        return windowModel;
    }

    private SlidingModel createSampleSlidingModel() {

        ParameterModel parameterModel = new ParameterModel();
        parameterModel.setName("slide");
        parameterModel.setDataType(DataType.NUMERIC);
        parameterModel.setDefaultValue("1");

        SlidingModel slidingModel = new SlidingModel();
        slidingModel.setName("CountbasedSlide");
        slidingModel.setClassName("gsn.beans.windowing.SlidingHandler2");
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
        parameterModel.setDefaultValue("auto");
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

    private Window createSampleWindow(WindowModel model) {
        Window window = new Window();

        Parameter parameter = new Parameter();
        parameter.setModel(model.getParameters().get(0));
        parameter.setValue("2");

        window.setModel(model);
        ArrayList<Parameter> parameters = new ArrayList<Parameter>();
        parameters.add(parameter);
        window.setParameters(parameters);

        return window;
    }

    private Sliding createSampleSliding(SlidingModel model) {
        Sliding sliding = new Sliding();

        Parameter parameter = new Parameter();
        parameter.setModel(model.getParameters().get(0));
        parameter.setValue("2");

        sliding.setModel(model);
        ArrayList<Parameter> parameters = new ArrayList<Parameter>();
        parameters.add(parameter);
        sliding.setParameters(parameters);

        return sliding;
    }

    private WrapperNode createSampleWrapperNode(WrapperModel model, Window window, Sliding sliding) {
        WrapperNode wrapperNode = new WrapperNode();
        wrapperNode.setName("test-wrapper-node");

        ArrayList<Parameter> parameters = new ArrayList<Parameter>();
        Parameter parameter = new Parameter();
        parameter.setModel(model.getParameters().get(0));
        parameter.setValue(parameter.getModel().getDefaultValue());
        parameters.add(parameter);

        parameter = new Parameter();
        parameter.setModel(model.getParameters().get(1));
        parameter.setValue(parameter.getModel().getDefaultValue());
        parameters.add(parameter);

        wrapperNode.setModel(model);
        wrapperNode.setParameters(parameters);

        wrapperNode.setSliding(sliding);
        wrapperNode.setWindow(window);

        return wrapperNode;
    }

    @Test
    public void testCreateSampleWindowModel() {
        Session session = HibernateUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();
        WindowModel windowModel = createSampleWindowModel();
        session.save(windowModel);
        tx.commit();
    }

    @Test
    public void testCreateSampleSlidingModel() {
        Session session = HibernateUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();
        SlidingModel slidingModel = createSampleSlidingModel();
        session.save(slidingModel);
        tx.commit();
    }

    @Test
    public void testCreateSampleWindowl() {
        Session session = HibernateUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();
        WindowModel windowModel = createSampleWindowModel();
        session.save(windowModel);

        Window window = createSampleWindow(windowModel);
        session.save(window);
        
        tx.commit();
    }

    @Test
    public void testCreateSampleSlidingl() {
        Session session = HibernateUtil.getCurrentSession();
        Transaction tx = session.beginTransaction();
        SlidingModel slidingModel = createSampleSlidingModel();
        session.save(slidingModel);

        Sliding sliding = createSampleSliding(slidingModel);
        session.save(sliding);
        
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


        SlidingModel slidingModel = createSampleSlidingModel();
        session.save(slidingModel);
        Sliding sliding = createSampleSliding(slidingModel);
        session.save(sliding);

        WindowModel windowModel = createSampleWindowModel();
        session.save(windowModel);
        Window window = createSampleWindow(windowModel);
        session.save(window);

        WrapperNode wrapperNode = createSampleWrapperNode(wrapperModel, window, sliding);
        session.save(wrapperNode);

        tx.commit();
    }

    @Test
    public void testHibernateUtil() {
        Session session = HibernateUtil.getCurrentSession();

        Transaction tx = session.beginTransaction();

        tx.commit();
    }
}
