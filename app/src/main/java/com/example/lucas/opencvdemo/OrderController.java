package com.example.lucas.opencvdemo;

import org.opencv.core.Point;

/**
 * Created by Lucas on 2016/10/21.
 */
public class OrderController {
    Point screen_old;
    Point current_old;
    String Order_old;
    public String updateOrder(double currentContourArea, Point screenCenterPoint,Point currentCenterPoint) {
    //    if (screenCenterPoint == this.screen_old && currentCenterPoint == current_old) {
    //    } else {
    //        this.screen_old = screenCenterPoint;
    //        this.current_old = currentCenterPoint;
    //    }
        System.out.println(screenCenterPoint);
        System.out.println(currentCenterPoint);
        System.out.println(currentContourArea);
        String order="s";
        if (currentContourArea <= 80000) {


            if((screenCenterPoint.y+40<currentCenterPoint.y)&&(currentCenterPoint.y<screenCenterPoint.y+330)){
                ;
            }
            else {
                if(screenCenterPoint.y+40>currentCenterPoint.y){
                    order="u";
                }
                if(screenCenterPoint.y+330<currentCenterPoint.y){
                    order="d";
                }
            }

            if((screenCenterPoint.x-150<currentCenterPoint.x)&&(currentCenterPoint.x<screenCenterPoint.x+150)){
                ;
            }
            else {
                if(screenCenterPoint.x-150>currentCenterPoint.x){
                    order=  "l";
                }
                if(screenCenterPoint.x+150<currentCenterPoint.x){
                    order= "r";
                }
            }


        }else{
            order="d";
        }
        this.Order_old= order;
        return order;
    }

}
