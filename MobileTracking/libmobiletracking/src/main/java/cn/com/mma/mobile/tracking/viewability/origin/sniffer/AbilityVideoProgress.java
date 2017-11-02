package cn.com.mma.mobile.tracking.viewability.origin.sniffer;

/**
 * 视频进度监测类型
 * Created by mma on 17/10/27.
 */
public enum AbilityVideoProgress {

    TRACK1_4("25"),
    TRACK2_4("50"),
    TRACK3_4("75"),
    TRACK4_4("100");
    private String value = "";

    AbilityVideoProgress(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
