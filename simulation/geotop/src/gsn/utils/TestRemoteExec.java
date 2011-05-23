package gsn.utils;

/* Testing remote exec of an ssh script
 */
public class TestRemoteExec {

    public static String command = "cd /data/hydrosys/final/LAFOULY-GEOTOP ; ./run.pl 20091010T0100 20091010T0200"; //"xeyes";
    public static String user = "sim";
    public static String password = "hydrosys";
    public static String hostname = "lsir-hydrosys01";
    public static int controlPort = 0;

    public static void main(String[] args) throws InterruptedException {
        RemoteExecThread aRemoteExecThread = new RemoteExecThread(command, hostname, user, password, controlPort);
        aRemoteExecThread.start();

        while  (aRemoteExecThread.isRunning()) {
                Thread.sleep(10000);
        }

        System.out.println("before");
        aRemoteExecThread.getRemoteFile("/data/hydrosys/final/LAFOULY-GEOTOP/plot_map/11_Txy/lay1/11_TxyL0001N0001.png","results");
        aRemoteExecThread.getRemoteFile("/data/hydrosys/final/LAFOULY-GEOTOP/plot_map/11_Txy/lay2/11_TxyL0002N0001.png","results");
        aRemoteExecThread.getRemoteFile("/data/hydrosys/final/LAFOULY-GEOTOP/plot_map/11_Txy/lay3/11_TxyL0003N0001.png","results");
        aRemoteExecThread.getRemoteFile("/data/hydrosys/final/LAFOULY-GEOTOP/plot_map/11_Txy/lay4/11_TxyL0004N0001.png","results");
        aRemoteExecThread.getRemoteFile("/data/hydrosys/final/LAFOULY-GEOTOP/plot_map/11_Txy/lay5/11_TxyL0005N0001.png","results");
        aRemoteExecThread.getRemoteFile("/data/hydrosys/final/LAFOULY-GEOTOP/plot_map/11_Txy/lay6/11_TxyL0006N0001.png","results");
        System.out.println("after");
    }

}
