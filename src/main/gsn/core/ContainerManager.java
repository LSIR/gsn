package gsn.core;

import gsn.beans.Operator;
import gsn.channels.DataChannel;
import gsn.channels.DefaultDataChannel;
import gsn.channels.ChannelConfig;
import gsn.utils.ChainedReponsibility;
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

public class ContainerManager extends ChainedReponsibility<OperatorConfig> {

    private static transient Logger                                logger                              = Logger.getLogger ( ContainerManager.class );

    private final ArrayList<OpStateChangeListener> changeListeners = new ArrayList<OpStateChangeListener>();

    private final ConcurrentHashMap<OperatorConfig,MutablePicoContainer> operators = new ConcurrentHashMap<OperatorConfig, MutablePicoContainer>();

    private final OpUnloader unloader = new OpUnloader();

    public void addOpStateChangeListener(OpStateChangeListener listener) {
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
        MutablePicoContainer picoOp = new PicoBuilder().withBehaviors(synchronizing(), caching()).withLifecycle().build();
        try {
            //Operator's configurations
            picoOp.addComponent(Operator.class, forName(operatorConfig.getClassName()));
            picoOp.addComponent(DataChannel.class);
            picoOp.addComponent(operatorConfig);
            picoOp.setName(operatorConfig.getIdentifier());
            //channels
            for (ChannelConfig cConfig:operatorConfig.getChannels()){
                MutablePicoContainer picoSource = picoOp.makeChildContainer();
                picoSource.addComponent(cConfig);
                picoSource.addComponent(DataChannel.class, DefaultDataChannel.class);
                picoSource.setName(cConfig.getName());
                MutablePicoContainer picoWrapper = picoSource.makeChildContainer();
                picoWrapper.addComponent(cConfig.getSourceConfig());
                picoWrapper.addComponent(cConfig.getSourceConfig().getClassName());
                picoWrapper.setName(cConfig.getSourceConfig().getClassName());
                picoWrapper.addComponent(Wrapper.class, forName(cConfig.getSourceConfig().getClassName()));
            }


            try{
                operators.put(operatorConfig,picoOp);
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

    public ChainedReponsibility<OperatorConfig> getUnloader() {
        return unloader;
    }

    private class OpUnloader extends ChainedReponsibility<OperatorConfig> {

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
            container = operators.remove(operatorConfig);
            fireOpUnloading(container);
            try{
                container.dispose();
            }catch (Exception e){
                logger.error("Operator unloading failed: "+e.getMessage(),e);
            }
            return true;
        }
    }
}
