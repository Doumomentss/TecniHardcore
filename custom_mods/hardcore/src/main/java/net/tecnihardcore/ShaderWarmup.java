package net.tecnihardcore;
/** Build the Overworld pipeline before opening a socket: a cold shader compile must not time out login. */
public final class ShaderWarmup {
 public static void prepare(){
  try{var iris=Class.forName("net.irisshaders.iris.Iris");var config=iris.getMethod("getIrisConfig").invoke(null);if(!Boolean.TRUE.equals(config.getClass().getMethod("areShadersEnabled").invoke(config)))return;var pack=(java.util.Optional<?>)iris.getMethod("getCurrentPack").invoke(null);if(pack.isEmpty())return;
   var id=Class.forName("net.irisshaders.iris.shaderpack.materialmap.NamespacedId");var dimension=id.getConstructor(String.class,String.class).newInstance("minecraft","overworld");var manager=iris.getMethod("getPipelineManager").invoke(null);Hardcore.LOG.info("Preparing shaders before connecting…");manager.getClass().getMethod("preparePipeline",id).invoke(manager,dimension);Hardcore.LOG.info("Shaders prepared before connecting");
  }catch(ClassNotFoundException ignored){}catch(Exception error){Hardcore.LOG.warn("Shader preparation failed: {}",error.getCause()==null?error:error.getCause());}
 }
}
