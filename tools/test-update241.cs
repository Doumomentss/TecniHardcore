using System;
using System.IO;
using System.Drawing;
using System.Windows.Forms;
using TecniHardcoreInstaller;
class UpdateIntegrationTest {
 [STAThread] static int Main(string[] args){
  string root=Path.GetFullPath(args[0]);string output=Path.GetFullPath(args[1]);Directory.CreateDirectory(output);
  string pak=Path.Combine(root,"chrome_100_percent.pak");
  var held=new FileStream(pak,FileMode.Open,FileAccess.ReadWrite,FileShare.None);
  bool waited=false;DateTime deadline=DateTime.UtcNow.AddMinutes(2);
  Application.EnableVisualStyles();Application.SetCompatibleTextRenderingDefault(false);
  var form=new UpdateForm(root,0);var timer=new Timer{Interval=200};
  timer.Tick+=(s,e)=>{
   string text="";foreach(Control c in form.Controls)if(c is Label)text+=c.Text+"\n";
   if(text.Contains("Esperando:")&&!waited){
    if(File.Exists(Path.Combine(root,".tecni-update-journal.txt")))throw new Exception("Replacement began before the pak was released");
    using(var bitmap=new Bitmap(form.Width,form.Height)){form.DrawToBitmap(bitmap,new Rectangle(Point.Empty,bitmap.Size));bitmap.Save(Path.Combine(output,"waiting-for-pak.png"));}
    File.WriteAllText(Path.Combine(output,"waiting.txt"),text);waited=true;held.Dispose();Console.WriteLine("PASS: visible updater waited before replacing the locked pak");
   }
   if(text.Contains("Actualización incompleta")||DateTime.UtcNow>deadline){held.Dispose();File.WriteAllText(Path.Combine(output,"failure.txt"),text);Environment.Exit(1);}
  };
  timer.Start();Application.Run(form);timer.Stop();timer.Dispose();held.Dispose();
  if(!waited||!File.ReadAllText(Path.Combine(root,"installation-version.txt")).Contains("2.4.1"))return 1;
  Console.WriteLine("PASS: installer committed launcher 2.4.1 after release of the lock");return 0;
 }
}
