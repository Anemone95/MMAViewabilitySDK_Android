package cn.com.mma.mobile.tracking.bean;

import java.util.HashMap;
import java.util.List;

/**
 * 对应sdkconfig.xml文件中的<company>内的<config>标签
 * 
 * @author lincoln
 */
public class Config {
    public List<Argument> arguments;
    public List<Event> events;
    public HashMap<String, Argument> adplacements;
    public HashMap<String, Argument> viewabilityarguments;



}