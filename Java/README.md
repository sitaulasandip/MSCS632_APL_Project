$env:Path += ';C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot\bin'


cd C:\Projects\MSCS632_APL_Project\Java



javac -d out src/main/java/com/mscs632/todolist/*.java

java -cp out com.mscs632.todolist.Main

cd C:\Projects\MSCS632_APL_Project\Java

$files = Get-ChildItem -Path 'src/main/java/com/mscs632/todolist' -Filter '*.java' |
    Select-Object -ExpandProperty FullName

javac -d out $files
java -cp out com.mscs632.todolist.Main