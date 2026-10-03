import java.lang.instrument.Instrumentation;
import com.sun.tools.attach.VirtualMachine;

/** Local, one-shot stop only. No listener, credential bypass or arbitrary command channel. */
public final class GracefulStop {
    public static void main(String[] args) throws Exception {
        if(args.length!=3)throw new IllegalArgumentException("PID agent.jar qa|production");
        VirtualMachine vm=VirtualMachine.attach(args[0]);
        try {vm.loadAgent(args[1],args[2]);} finally {vm.detach();}
    }
    public static void agentmain(String mode,Instrumentation instrumentation) throws Exception {
        boolean metrics=mode.endsWith("-metrics");if(metrics)mode=mode.substring(0,mode.length()-8);
        boolean qa=Boolean.getBoolean("tecni.testServer");
        if(!mode.equals(qa?"qa":"production"))throw new IllegalStateException("Wrong server mode");
        if(!qa&&!Boolean.getBoolean("tecni.allowTrialBoss"))throw new IllegalStateException("Unknown production host");
        for(Class<?> type:instrumentation.getAllLoadedClasses())if(type.getName().equals("net.tecnihardcore.Hardcore")) {
            Object server=type.getField("server").get(null);
            if(!(server instanceof java.util.concurrent.Executor executor))throw new IllegalStateException("Server not ready");
            if(metrics){executor.execute(()->{try{float ms=(Float)server.getClass().getMethod("method_3830").invoke(server);System.out.println("TECNI_METRICS tickMeanMs="+ms);}catch(Exception error){throw new IllegalStateException(error);}});return;}
            var stop=server.getClass().getMethod("method_3747",boolean.class);
            executor.execute(()->{try{stop.invoke(server,false);}catch(Exception error){throw new IllegalStateException(error);}});
            return;
        }
        throw new IllegalStateException("TecniHardcore host missing");
    }
}
