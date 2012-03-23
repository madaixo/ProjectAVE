cd .\build\classes
java -Dsun.java2d.opengl=True -Dsun.java2d.opengl.fbobject=false halfNES %1
pause
rem Add -XX:+PrintCompilation to see JVM opts