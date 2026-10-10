using System;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Security.Cryptography;
using System.Drawing;
using System.Windows.Forms;
using System.Threading.Tasks;
using System.Diagnostics;
using System.Collections.Generic;
using System.Threading;
namespace TecniHardcoreInstaller {
static class Payload {
 static void Shortcut(string root) {
  try {
   string desktop=Environment.GetFolderPath(Environment.SpecialFolder.DesktopDirectory);Directory.CreateDirectory(desktop);
   Type type=Type.GetTypeFromProgID("WScript.Shell");object shell=Activator.CreateInstance(type);
   object link=type.InvokeMember("CreateShortcut",BindingFlags.InvokeMethod,null,shell,new object[]{Path.Combine(desktop,"TecniHardcore.lnk")});
   Type linkType=link.GetType();string exe=Path.Combine(root,"TecniHardcore Launcher.exe");
   linkType.InvokeMember("TargetPath",BindingFlags.SetProperty,null,link,new object[]{exe});
   linkType.InvokeMember("WorkingDirectory",BindingFlags.SetProperty,null,link,new object[]{root});
   linkType.InvokeMember("IconLocation",BindingFlags.SetProperty,null,link,new object[]{exe+",0"});
   linkType.InvokeMember("Description",BindingFlags.SetProperty,null,link,new object[]{"TecniHardcore 1.20.1 Fabric"});
   linkType.InvokeMember("Save",BindingFlags.InvokeMethod,null,link,null);
   System.Runtime.InteropServices.Marshal.FinalReleaseComObject(link);System.Runtime.InteropServices.Marshal.FinalReleaseComObject(shell);
  }catch(Exception error){try{File.WriteAllText(Path.Combine(Path.GetTempPath(),"tecnihardcore-shortcut-error.txt"),error.Message);}catch{}}
 }
 const string Journal=".tecni-update-journal.txt";
 static string Digest(string file) {using(var sha=SHA256.Create())using(var input=File.OpenRead(file))return BitConverter.ToString(sha.ComputeHash(input)).Replace("-","").ToLowerInvariant();}
 static string Target(string root,string relative) {
  if(String.IsNullOrWhiteSpace(relative)||Path.IsPathRooted(relative)||relative.IndexOf(':')>=0)throw new Exception("Ruta inválida en el paquete: "+relative);
  string normalized=relative.Replace('/',Path.DirectorySeparatorChar);
  foreach(string part in normalized.Split(Path.DirectorySeparatorChar))if(part==".."||part=="."||part.Length==0)throw new Exception("Ruta inválida en el paquete: "+relative);
  string first=normalized.Split(Path.DirectorySeparatorChar)[0];
  if(first.Equals("game",StringComparison.OrdinalIgnoreCase)||first.Equals("backups",StringComparison.OrdinalIgnoreCase)||first.StartsWith(".tecni",StringComparison.OrdinalIgnoreCase))throw new Exception("El paquete intenta modificar datos personales.");
  string target=Path.GetFullPath(Path.Combine(root,normalized));
  if(!target.StartsWith(root.TrimEnd(Path.DirectorySeparatorChar)+Path.DirectorySeparatorChar,StringComparison.OrdinalIgnoreCase))throw new Exception("Ruta fuera de la instalación.");
  for(string check=Path.GetDirectoryName(target);check!=null;check=Path.GetDirectoryName(check)) {
   if(Directory.Exists(check)&&(File.GetAttributes(check)&FileAttributes.ReparsePoint)!=0)throw new Exception("La carpeta de instalación contiene un enlace. Elige otra carpeta.");
   if(check.Equals(root,StringComparison.OrdinalIgnoreCase))break;
  }
  if(File.Exists(target)&&(File.GetAttributes(target)&FileAttributes.ReparsePoint)!=0)throw new Exception("Archivo enlazado no permitido: "+relative);
  return target;
 }
 static void Record(string journal,string line) {using(var stream=new FileStream(journal,FileMode.Append,FileAccess.Write,FileShare.Read))using(var writer=new StreamWriter(stream)){writer.WriteLine(line);writer.Flush();stream.Flush(true);}}
 static void Recover(string root) {
  string journal=Path.Combine(root,Journal);if(!File.Exists(journal))return;
  string[] lines=File.ReadAllLines(journal);if(lines.Length==0)throw new Exception("Registro de recuperación incompleto.");
  string stamp=lines[0];if(!System.Text.RegularExpressions.Regex.IsMatch(stamp,"^[a-f0-9]{32}$"))throw new Exception("Registro de recuperación inválido.");
  for(int i=lines.Length-1;i>0;i--) {
   if(lines[i].Length<3)continue;
   string relative=lines[i].Substring(2),target=Target(root,relative);
   string backup=Path.Combine(root,"backups",stamp,relative.Replace('/',Path.DirectorySeparatorChar));
   if(lines[i][0]=='1'){if(File.Exists(backup))File.Copy(backup,target,true);}
   else if(lines[i][0]=='0'&&File.Exists(target))File.Delete(target);
   if(File.Exists(target+".tecni-next"))File.Delete(target+".tecni-next");
  }
  File.Delete(journal);
 }
 static void Replace(string root,string stage,string relative,string stamp) {
  string target=Target(root,relative),source=Target(stage,relative);
  bool existed=File.Exists(target);
  if(existed&&Digest(target)==Digest(source))return;
  Directory.CreateDirectory(Path.GetDirectoryName(target));
  if(existed){string backup=Path.Combine(root,"backups",stamp,relative.Replace('/',Path.DirectorySeparatorChar));Directory.CreateDirectory(Path.GetDirectoryName(backup));File.Copy(target,backup,true);}
  Record(Path.Combine(root,Journal),(existed?"1 ":"0 ")+relative);
  string next=target+".tecni-next";File.Copy(source,next,true);
  if(existed)File.Replace(next,target,null);else File.Move(next,target);
 }
 internal static void WaitForFiles(string root,List<string> files,Action<string> status,bool wait,int timeoutMs=180000) {
  var timer=Stopwatch.StartNew();
  while(true) {
   try{foreach(string relative in files){string target=Target(root,relative);if(File.Exists(target))using(var file=new FileStream(target,FileMode.Open,FileAccess.ReadWrite,FileShare.None)) {}}return;}
   catch(IOException error){if(!wait||timer.ElapsedMilliseconds>=timeoutMs)throw new IOException("Otro proceso sigue utilizando archivos de esta instalación. Cierra todas las ventanas de TecniHardcore de esta carpeta y pulsa REINTENTAR. No se instalaron archivos nuevos.",error);
    status("Esperando: cierra las otras ventanas de TecniHardcore. No abras el acceso directo todavía.");Thread.Sleep(500);}
  }
 }
 public static void Install(string destination,Action<string> status) {Install(destination,status,false);}
 private static void Install(string destination,Action<string> status,bool waitForLocks) {
  string root=Path.GetFullPath(destination).TrimEnd(Path.DirectorySeparatorChar);
  if(Path.GetPathRoot(root).TrimEnd(Path.DirectorySeparatorChar)==root)throw new Exception("Elige una carpeta, no la raíz del disco.");
  Directory.CreateDirectory(root);
  string mutexName="Local\\TecniHardcoreInstall-"+BitConverter.ToString(SHA256.Create().ComputeHash(System.Text.Encoding.UTF8.GetBytes(root.ToLowerInvariant()))).Replace("-","");
  using(var mutex=new Mutex(false,mutexName)) {
   bool acquired=false;
   try {try {acquired=mutex.WaitOne(0);}catch(AbandonedMutexException){acquired=true;}
    if(!acquired)throw new Exception("Ya hay una instalación en curso en esta carpeta.");
    Recover(root);
    string stamp=Guid.NewGuid().ToString("N"),stage=Path.Combine(root,".tecni-stage-"+stamp);
    Directory.CreateDirectory(stage);
    try {
     var files=new List<string>();var expected=new Dictionary<string,string>(StringComparer.OrdinalIgnoreCase);
     using(Stream embedded=Assembly.GetExecutingAssembly().GetManifestResourceStream("launcher.zip"))
     using(var zip=new ZipArchive(embedded,ZipArchiveMode.Read)) {
      var sums=zip.GetEntry("SHA256SUMS.txt");if(sums==null)throw new Exception("Falta el manifiesto de integridad.");
      using(var reader=new StreamReader(sums.Open()))while(!reader.EndOfStream) {
       string line=reader.ReadLine();if(String.IsNullOrWhiteSpace(line))continue;
       if(line.Length<67||!System.Text.RegularExpressions.Regex.IsMatch(line.Substring(0,64),"^[a-f0-9]{64}$")||line.Substring(64,2)!="  ")throw new Exception("Manifiesto inválido.");
       string relative=line.Substring(66);Target(root,relative);expected.Add(relative,line.Substring(0,64));
      }
      long expanded=0;
      foreach(var entry in zip.Entries) {
       if(entry.FullName.EndsWith("/"))continue;
       if(entry.FullName!="SHA256SUMS.txt"&&!expected.ContainsKey(entry.FullName))throw new Exception("Archivo fuera del manifiesto: "+entry.FullName);
       if(files.Contains(entry.FullName))throw new Exception("Archivo duplicado en el paquete.");
       expanded+=entry.Length;if(expanded>4L*1024*1024*1024)throw new Exception("Paquete demasiado grande.");
       string target=Target(stage,entry.FullName);Directory.CreateDirectory(Path.GetDirectoryName(target));
       status("Verificando "+entry.FullName);
       using(var input=entry.Open())using(var output=File.Create(target))input.CopyTo(output);
       if(entry.FullName!="SHA256SUMS.txt"&&Digest(target)!=expected[entry.FullName])throw new Exception("Falló SHA-256 de "+entry.FullName+". Ningún archivo instalado ha cambiado.");
       files.Add(entry.FullName);
      }
      if(files.Count!=expected.Count+1)throw new Exception("Faltan archivos en el paquete.");
     }
     // Check locks before the first replacement, including running Electron subprocesses.
     WaitForFiles(root,files,status,waitForLocks);
     string journal=Path.Combine(root,Journal);Record(journal,stamp);
     try {
      foreach(string relative in files){status("Instalando "+relative);Replace(root,stage,relative,stamp);}
      const string versionFile="installation-version.txt";
      File.WriteAllText(Path.Combine(stage,versionFile),"TecniHardcore "+BuildInfo.Version+" / Minecraft 1.20.1 Fabric / mecánicas "+BuildInfo.PackVersion+"\r\n");
      Replace(root,stage,versionFile,stamp);
      // Read back the installed payload before committing or reopening the launcher.
      foreach(var item in expected)if(Digest(Target(root,item.Key))!=item.Value)throw new IOException("La instalación no superó la verificación final de "+item.Key);
      File.Delete(journal);
      Shortcut(root);
     }catch{Recover(root);throw;}
    } finally {Directory.Delete(stage,true);}
   } finally {if(acquired)mutex.ReleaseMutex();}
  }
 }
 public static void Update(string destination,int launcherPid) {Update(destination,launcherPid,s=>{});}
 public static void Update(string destination,int launcherPid,Action<string> status) {
  status("Cerrando el launcher anterior…");
  if(launcherPid>0)try{using(var previous=Process.GetProcessById(launcherPid)){if(!previous.WaitForExit(60000))throw new Exception("El launcher no se cerró. Cierra Minecraft y vuelve a intentar.");}}catch(ArgumentException){}
  // Extract once, then wait for every installed file before starting the transaction.
  Install(destination,status,true);
  status("Actualización verificada. Abriendo el launcher actualizado…");
  Process.Start(new ProcessStartInfo(Path.Combine(destination,"TecniHardcore Launcher.exe")){WorkingDirectory=destination});
 }
}
class UpdateForm:Form {
 readonly string destination;readonly int previousPid;Label status=new Label();Button retry=new Button();ProgressBar progress=new ProgressBar();bool working;
 public UpdateForm(string folder,int pid){
  destination=folder;previousPid=pid;Text="Actualizando TecniHardcore "+BuildInfo.Version;Size=new Size(680,320);StartPosition=FormStartPosition.CenterScreen;BackColor=Color.FromArgb(14,16,24);ForeColor=Color.White;Font=new Font("Segoe UI",10);FormBorderStyle=FormBorderStyle.FixedDialog;MaximizeBox=false;
  Controls.Add(new Label{Text="INSTALANDO TECNIHARDCORE "+BuildInfo.Version,AutoSize=true,Font=new Font("Segoe UI",16,FontStyle.Bold),ForeColor=Color.Goldenrod,Location=new Point(22,18)});
  Controls.Add(new Label{Text="La descarga terminó. Espera a que esta ventana instale y vuelva a abrir el launcher.\nNo abras el acceso directo durante la instalación.",Location=new Point(24,62),Size=new Size(620,46)});
  Controls.Add(new Label{Text="Carpeta: "+folder,Location=new Point(24,114),Size=new Size(620,34)});
  status.SetBounds(24,153,620,52);Controls.Add(status);progress.SetBounds(24,210,620,15);Controls.Add(progress);
  retry.Text="REINTENTAR";retry.SetBounds(484,239,160,30);retry.Click+=(s,e)=>StartUpdate();Controls.Add(retry);
  Shown+=(s,e)=>StartUpdate();FormClosing+=(s,e)=>{if(working)e.Cancel=true;};
 }
 async void StartUpdate(){if(working)return;working=true;retry.Enabled=false;progress.Style=ProgressBarStyle.Marquee;
  try{await Task.Run(()=>Payload.Update(destination,previousPid,msg=>BeginInvoke(new Action(()=>status.Text=msg))));working=false;Close();}
  catch(Exception error){string detail=Path.Combine(Path.GetTempPath(),"tecnihardcore-install-error.txt");try{File.WriteAllText(detail,error.ToString());}catch{}
   status.Text="Actualización incompleta: "+error.Message+" Detalle: "+detail;progress.Style=ProgressBarStyle.Blocks;progress.Value=0;}
  finally{working=false;if(!IsDisposed)retry.Enabled=true;}
 }
}
class InstallerForm:Form {
 TextBox destination=new TextBox();Label status=new Label();Button install=new Button();bool finished;string installedFolder;
 public InstallerForm(){
  Text="TecniHardcore "+BuildInfo.Version;Size=new Size(640,330);StartPosition=FormStartPosition.CenterScreen;BackColor=Color.FromArgb(14,16,24);ForeColor=Color.White;Font=new Font("Segoe UI",10);FormBorderStyle=FormBorderStyle.FixedDialog;MaximizeBox=false;
  Controls.Add(new Label{Text="TECNIHARDCORE",AutoSize=true,Font=new Font("Segoe UI",20,FontStyle.Bold),ForeColor=Color.Goldenrod,Location=new Point(24,20)});
  Controls.Add(new Label{Text="Minecraft 1.20.1 · Santuarios 3D · Launcher con actualizaciones",AutoSize=true,Location=new Point(26,70)});
  destination.Text=Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"TecniHardcore");destination.SetBounds(26,112,560,30);Controls.Add(destination);
  var browse=new Button{Text="Carpeta…",Location=new Point(26,150),Width=110};browse.Click+=(s,e)=>{using(var dialog=new FolderBrowserDialog()){dialog.SelectedPath=destination.Text;if(dialog.ShowDialog()==DialogResult.OK)destination.Text=dialog.SelectedPath;}};Controls.Add(browse);
  status.SetBounds(26,193,560,40);status.Text="Conserva tus ajustes. Java 17 se verifica al abrir el juego.";Controls.Add(status);
  install.Text="INSTALAR / REPARAR";install.SetBounds(350,238,236,35);install.Click+=async(s,e)=>{
   if(finished){Process.Start(new ProcessStartInfo(Path.Combine(installedFolder,"TecniHardcore Launcher.exe")){WorkingDirectory=installedFolder});Close();return;}
   install.Enabled=false;browse.Enabled=false;destination.Enabled=false;
   try{string folder=destination.Text;await Task.Run(()=>Payload.Install(folder,msg=>BeginInvoke(new Action(()=>status.Text=msg))));status.Text="Instalación verificada. Ya puedes abrir el launcher.";install.Text="ABRIR LAUNCHER";finished=true;installedFolder=folder;}
   catch(Exception error){status.Text="Error: "+error.Message;MessageBox.Show(error.Message,"Instalación incompleta",MessageBoxButtons.OK,MessageBoxIcon.Error);}
   finally{install.Enabled=true;browse.Enabled=true;destination.Enabled=true;}
  };Controls.Add(install);
  FormClosing+=(s,e)=>{if(!install.Enabled)e.Cancel=true;};
 }
 [STAThread] static int Main(string[] args){
  bool update=args.Length==3&&args[0]=="--update";
  if(update){Application.EnableVisualStyles();Application.SetCompatibleTextRenderingDefault(false);Application.Run(new UpdateForm(args[1],Int32.Parse(args[2])));return 0;}
  if(args.Length==2&&args[0]=="--dir"){try{Payload.Install(args[1],s=>{});return 0;}catch(Exception e){File.WriteAllText(Path.Combine(Path.GetTempPath(),"tecnihardcore-install-error.txt"),e.ToString());return 1;}}
  Application.EnableVisualStyles();Application.SetCompatibleTextRenderingDefault(false);Application.Run(new InstallerForm());return 0;
 }
}
}
