package com.soumyajit.gradlemc.lock;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class InstanceLockIO {
    public static final int MAX_BYTES=2*1024*1024;
    private InstanceLockIO(){ }
    public static void write(Path path,InstanceLockSnapshot value)throws IOException{Path target=path.toAbsolutePath().normalize(),parent=target.getParent();Files.createDirectories(parent);if(Files.isSymbolicLink(parent))throw new IOException("Lock directory cannot be a symbolic link");String json=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(value);byte[] bytes=json.getBytes(StandardCharsets.UTF_8);if(bytes.length>MAX_BYTES)throw new IOException("Instance lock exceeds safe size limit");Path temp=Files.createTempFile(parent,".instance-lock-",".tmp");try{Files.write(temp,bytes,StandardOpenOption.TRUNCATE_EXISTING);try{Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}}
    public static InstanceLockSnapshot read(Path path)throws IOException{Path value=path.toAbsolutePath().normalize();if(!Files.isRegularFile(value,LinkOption.NOFOLLOW_LINKS)||Files.isSymbolicLink(value))throw new IOException("Instance lock is unavailable or unsafe");if(Files.size(value)>MAX_BYTES)throw new IOException("Instance lock exceeds safe read limit");try{InstanceLockSnapshot lock=new Gson().fromJson(Files.readString(value,StandardCharsets.UTF_8),InstanceLockSnapshot.class);if(lock==null||lock.schemaVersion()!=1)throw new IOException("Unsupported instance lock schema");return lock;}catch(RuntimeException e){throw new IOException("Malformed instance lock",e);}}
}
