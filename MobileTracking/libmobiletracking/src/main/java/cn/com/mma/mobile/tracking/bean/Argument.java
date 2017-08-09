package cn.com.mma.mobile.tracking.bean;

/**
 * 对应sdkconfig.xml文件中的<arguments>标签
 *
 * @author lincoln
 */
public class Argument {
    public String key = "";
    public String value = "";
    public boolean urlEncode;
    public boolean isRequired;

    @Override
    public String toString() {
        return "[ " + "key:" + key + ",value:" + value + ",encode:" + urlEncode + ",require:" + isRequired + " ]";
    }
}
