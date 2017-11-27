package com.sdk.mz.testweakref;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;

import java.lang.ref.WeakReference;

public class MainActivity extends AppCompatActivity {
    private static final int MESSAGE_ONEXPOSE = 0x102;
    Car ddd = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    public void main(String[] args) {
        System.out.println("Hello World!");
        Car car = new Car(2000.0, "red");
        WeakReference<Car> wrc = new WeakReference(car);
        //wrc.setStr("111");
        Car bbb = wrc.get();


        Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {//覆盖handleMessage方法
                final int what = msg.what;
                switch (what) {
                    case 0:
                    case MESSAGE_ONEXPOSE:
                        ddd = (Car) msg.obj;
                        ddd.toString();
//                        Bundle bundle = msg.getData();
//                        handlerViewAbilityMonitor(adView, bundle);
                        break;
                    default:
                        break;
                }
            }
        };

        Message message = mHandler.obtainMessage(MESSAGE_ONEXPOSE);
        message.obj = bbb;
        mHandler.sendMessage(message);


        int i = 0;
        while (true)
        {
            if (wrc.get() != null)
            {
                i++;
                Car ccc = wrc.get();
                ccc.toString();
                ddd.toString();
                //ccc.getColor();
                // System.gc();
                System.out.println("WeakReferenceCar's Car is alive for " + i + ", loop - " + wrc);
            }
            else
            {
//                System.out.println("WeakReferenceCar's Car has bean collected");
                //if(ccc != null)
                {
                    System.out.println("WeakReferenceCar's bbb has bean collected");
//                    bbb.toString();
                    // } else {
//                    System.out.println("WeakReferenceCar's ccc has bean collected");
                    break;
                }
            }
        }
    }
}

class Car
{
    private double     price;
    private String    color;
    public Car(double price, String color)
    {
        this.price = price;
        this.color = color;
    }

    public double getPrice()
    {
        return price;
    }

    public String getColor()
    {
        return color;
    }

    public String toString()
    {
        System.out.println("This car is a " + this.color + " car, costs $" + price);
        return "This car is a " + this.color + " car, costs $" + price;
    }
}


