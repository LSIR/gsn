package gsn.core;

import gsn.beans.Operator;
import gsn.channels.DataChannel;
import gsn.channels.DefaultDataChannel;
import gsn.channels.GSNChannel;
import gsn.channels.ChannelConfig;
import gsn.utils.ChainOfReponsibility;
import gsn2.wrappers.Wrapper;
import gsn.core.OperatorConfig;
import org.apache.log4j.Logger;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoBuilder;
import static org.picocontainer.behaviors.Behaviors.caching;
import static org.picocontainer.behaviors.Behaviors.synchronizing;

import static java.lang.Class.forName;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ContainerManager extends ChainOfReponsibility<OperatorConfig>{

    private static transient Logger                                logger                              = Logger.getLogger ( ContainerManager.class );

    private final ArrayList<OpStateChangeListener> changeListeners = new ArrayList<OpStateChangeListener>();

    private final ConcurrentHashMap<OperatorConfig,MutablePicoContainer> operators = new ConcurrentHashMap<OperatorConfig, MutablePicoContainer>();

    private final OpUnloader unloader = new OpUnloader();

    public void addOpStateChangeListener(OpStateChangeListener listener) {
        if (!changeListeners.contains(listener))
            changeListeners.add(listener);
    }

    public void removeOpStateChangeListener(OpStateChangeListener listener) {
        changeListeners.remove(listener);
    }

    private void fireOpLoading(MutablePicoContainer config) {
        for (OpStateChangeListener listener : changeListeners)
            try{
                listener.opLoading(config);
            }catch (Exception e ) {  
                logger.error(e.getMessage(),e);
            }

    }

    protected boolean handle(OperatorConfig operatorConfig) {
        MutablePicoContainer picoOp = new PicoBuilder().withBehaviors(synchronizing(), caching()).withLifecycle().withMonitor(new OperatorMonitor()).build();
        try {
            //Operator's configurations
            picoOp.addComponent(Operator.class, forName(operatorConfig.getClassName()));
            picoOp.addComponent(DataChannel.class, new GSNChannel(operatorConfig));
            picoOp.addComponent(operatorConfig);
            picoOp.setName(operatorConfig.getIdentifier());
            picoOp.getComponent(Operator.class);
            //channels
            for (ChannelConfig cConfig:operatorConfig.getChannels()){
                //for the channel
                MutablePicoContainer picoSource = picoOp.makeChildContainer();
                picoSource.addComponent(cConfig);
                picoSource.addComponent(DataChannel.class, DefaultDataChannel.class);
                picoSource.setName(cConfig.getName());
                picoSource.getComponent(DataChannel.class);
                //for the wrapper
                MutablePicoContainer picoWrapper = picoSource.makeChildContainer();
                picoWrapper.addComponent(cConfig.getSourceConfig());
                picoWrapper.addComponent(cConfig.getSourceConfig().getClassName());
                picoWrapper.setName(cConfig.getSourceConfig().getClassName());
                picoWrapper.addComponent(Wrapper.class, forName(cConfig.getSourceConfig().getClassName()));
                picoWrapper.getComponent(Wrapper.class);
            }


            try{
                operators.put(operatorConfig,picoOp);
                fireOpLoading(picoOp);
                picoOp.start();
            }catch (Exception e2){
                logger.error("Operator starting failed: "+e2.getMessage(),e2);
                getUnloader().proccess(operatorConfig);
                return false;
            }

        } catch (Exception e) {
            logger.error("Operator loading failed: "+e.getMessage(),e);
            return false;
        }
        return true;
    }

    public ConcurrentHashMap<OperatorConfig, MutablePicoContainer> getOperators() {
        return operators;
    }

    public ChainOfReponsibility<OperatorConfig> getUnloader() {
        return unloader;
    }

    private class OpUnloader extends ChainOfReponsibility<OperatorConfig>{

        private void fireOpUnloading(MutablePicoContainer config) {
            for (OpStateChangeListener listener : changeListeners)
                try{
                    listener.opUnLoading(config);
                }catch (Exception e ) {
                    logger.error(e.getMessage(),e);
                }
        }

        protected boolean handle(OperatorConfig operatorConfig) {
            MutablePicoContainer container  = operators.get(operatorConfig);
            fireOpUnloading(container);
            container = operators.remove(operatorConfig);
            try{
                container.dispose();
            }catch (Exception e){
                logger.error("Operator unloading failed: "+e.getMessage(),e);
            }
            return true;
        }
    }
}
