using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using System.Collections.Generic;
using TecniHardcoreInstaller;
class TransactionTests {
 static void Check(bool good,string message){if(!good)throw new Exception(message);}
 static int Main(string[] args) {
  string root=Path.GetFullPath(args[0]);Directory.CreateDirectory(root);
  File.WriteAllText(Path.Combine(root,"fixture.txt"),"old");
  File.WriteAllText(Path.Combine(root,"installation-version.txt"),"old-version");
  Directory.CreateDirectory(Path.Combine(root,"game"));File.WriteAllText(Path.Combine(root,"game","options.txt"),"music:0.123");
  // This file is written last. Its lock forces a failure after payload replacement.
  using(var locked=new FileStream(Path.Combine(root,"installation-version.txt"),FileMode.Open,FileAccess.ReadWrite,FileShare.None)) {
   bool failed=false;try{Payload.Install(root,s=>{});}catch(IOException){failed=true;}
   Check(failed,"fault injection did not fail");
   Check(File.ReadAllText(Path.Combine(root,"fixture.txt"))=="old","rollback failed");
   Check(!File.Exists(Path.Combine(root,"new.txt")),"rollback left newly created file");
  }
  Payload.Install(root,s=>{});
  Check(File.ReadAllText(Path.Combine(root,"fixture.txt"))=="new","install failed");
  Check(File.ReadAllText(Path.Combine(root,"game","options.txt"))=="music:0.123","preferences changed");
  // A persisted journal models interruption between replacing a file and commit.
  string stamp=new string('a',32),backup=Path.Combine(root,"backups",stamp);Directory.CreateDirectory(backup);
  File.WriteAllText(Path.Combine(backup,"recovery.txt"),"before-interruption");
  File.WriteAllText(Path.Combine(root,"recovery.txt"),"interrupted");
  File.WriteAllText(Path.Combine(root,"created.txt"),"partial");
  File.WriteAllText(Path.Combine(root,".tecni-update-journal.txt"),stamp+"\n1 recovery.txt\n0 created.txt\n");
  Payload.Install(root,s=>{});
  Check(File.ReadAllText(Path.Combine(root,"recovery.txt"))=="before-interruption","crash recovery failed");
  Check(!File.Exists(Path.Combine(root,"created.txt")),"crash recovery left new file");
  Check(!File.Exists(Path.Combine(root,".tecni-update-journal.txt")),"journal not cleared");
  // Reproduce the reported Windows .pak lock, then release it like Chromium exiting.
  string pak=Path.Combine(root,"chrome_100_percent.pak");File.WriteAllText(pak,"old-pak");
  var held=new FileStream(pak,FileMode.Open,FileAccess.ReadWrite,FileShare.None);int waiting=0;
  var release=Task.Run(()=>{Thread.Sleep(1300);held.Dispose();});
  Payload.WaitForFiles(root,new List<string>{"chrome_100_percent.pak"},msg=>{waiting++;Check(!File.Exists(Path.Combine(root,".tecni-update-journal.txt")),"writes started while a launcher file was locked");},true,5000);
  release.Wait();Check(waiting>0,"update did not wait for the held pak");
  using(var locked=new FileStream(pak,FileMode.Open,FileAccess.ReadWrite,FileShare.None)){
   bool failed=false;try{Payload.WaitForFiles(root,new List<string>{"chrome_100_percent.pak"},msg=>{},true,10);}catch(IOException e){failed=e.Message.Contains("REINTENTAR");}
   Check(failed,"timeout did not explain how to retry");
  }
  Check(File.ReadAllText(pak)=="old-pak","locked file was modified");
  Console.WriteLine("PASS: rollback after replacement, crash recovery, exact backup and preserved game preferences");return 0;
 }
}
