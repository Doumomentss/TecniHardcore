from pathlib import Path
import hashlib,subprocess,zipfile,uuid
root=Path(__file__).resolve().parents[1]
work=root/'tools/test-runtime/updater-fixtures';work.mkdir(parents=True,exist_ok=True)
csc=Path('C:/Windows/Microsoft.NET/Framework64/v4.0.30319/csc.exe')
common=[str(csc),'/nologo','/reference:System.Windows.Forms.dll','/reference:System.Drawing.dll','/reference:System.IO.Compression.dll','/reference:System.IO.Compression.FileSystem.dll']
sources=[str(root/'installer/Installer.cs'),str(root/'installer/BuildInfo.cs')]
def fixture(name,values,corrupt=False):
    archive=work/(name+'.zip')
    sums=''.join(hashlib.sha256(value).hexdigest()+'  '+key+'\n' for key,value in values.items())
    with zipfile.ZipFile(archive,'w') as z:
        for key,value in values.items():z.writestr(key,value+b'corrupt' if corrupt else value)
        z.writestr('SHA256SUMS.txt',sums)
    return archive
archive=fixture('valid',{'fixture.txt':b'new','new.txt':b'created'})
out=work/'transactions.exe'
subprocess.run(common+['/target:exe','/main:TransactionTests','/out:'+str(out),'/resource:'+str(archive)+',launcher.zip']+sources+[str(root/'tools/test-installer-transactions.cs')],check=True)
subprocess.run([str(out),str(work/('installed-'+uuid.uuid4().hex))],check=True)
for name,archive in [('corrupt',fixture('corrupt',{'fixture.txt':b'new'},True)),('escape',fixture('escape',{'../escape.txt':b'evil'}))]:
    out=work/(name+'.exe');dest=work/(name+'-installed');dest.mkdir(exist_ok=True);(dest/'fixture.txt').write_text('old')
    subprocess.run(common+['/target:winexe','/out:'+str(out),'/resource:'+str(archive)+',launcher.zip']+sources,check=True)
    result=subprocess.run([str(out),'--dir',str(dest)])
    assert result.returncode==1,name+' should fail'
    assert (dest/'fixture.txt').read_text()=='old'
    assert not (work/'escape.txt').exists()
print('PASS: invalid hash and ZIP path traversal fail before modifying installed files')
