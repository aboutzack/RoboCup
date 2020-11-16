package CSU_Yunlu_2020;

import adf.agent.info.AgentInfo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LogHelper {
    public static final boolean on = false;
    private boolean logCreated = false;
    private String url;
    private BufferedWriter output;
    private AgentInfo agentInfo;
    private String name;

    public LogHelper(String url, AgentInfo agentInfo, String name){
        this.url = url;
        this.agentInfo = agentInfo;
        this.name = name;
        try{
            createLog();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }

    private void createLog() throws IOException {
        String fileName = url;
        File file = new File(fileName);
        if(!file.exists()){
            file.mkdirs();
        }
        fileName = url+"/"+agentInfo.getID()+".txt";
        file = new File(fileName);
        if(file.exists()){
            file.delete();
        }
        file.createNewFile();
        FileWriter writer = new FileWriter(file,true);
        this.output = new BufferedWriter(writer);
        logCreated = true;
    }

    public void write(String msg){
        if (logCreated) {
            try{
                output.write(msg+"\n");
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public void flush(){
        if(logCreated){
            try {
                output.flush();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public void writeAndFlush(String msg){
        try {
            output.write(name+" 第["+agentInfo.getTime()+"]回合 ("+agentInfo.getID()+"):"+msg+"\n");
            output.flush();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }
}
