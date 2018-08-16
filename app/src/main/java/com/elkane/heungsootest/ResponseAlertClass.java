/*
 * ***************************************************************************************
 *  ** Hyundai Motor India Engineering Pvt Ltd
 *  ** Electronics Engineering Dept-1., - Software Development Team
 *  ** Do Not Copy Without Prior Permission
 *  *****************************************************************************************
 *  ** Project Name: ESDS Development
 *  ** Target: Proof Of Concept (NU Head Unit)
 *  ** File Name: ResponseAlertClass.java
 *  ** @Author: Sivaram Boina
 *  ** @Co-Author: Sai Sriram Madhiraju
 *  ** Completion Date: 29-12-2017
 *  ***************************************************************************************
 */
package com.elkane.heungsootest;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Class for showing response alert(dialog) when the user sends request to the BLE unit (Here: LGIT)
 */
public class ResponseAlertClass {

    public static LayoutInflater sResponseLayoutInflater;
    public static View sResponseLayoutAlertDialogView;
    public static AlertDialog sResponseLayoutAlertDialog;
    public static Context sResponseLayoutContext;

    public static Button sButtonResponseALert;
    public static TextView sTextViewResponseAlert;
    public static ProgressBar sProgressBar;

    /**
     * Method creates and the alert based on the rquest from other activities/screens
     * @param activity Activity from which the request to show the dialog is received
     * @param context context from which the request to show the dialog is received
     * @param alerttext text that is to be displayed in the alert dialog
     * @param buttonText button text to be shown when the aler is displayed
     * @param progressBarStatus to show any ongoing process if any is running
     */
    public static void showResponseDialog(final Activity activity, Context context,String alerttext,String buttonText,boolean progressBarStatus)
    {
        sResponseLayoutContext=context;
        sResponseLayoutInflater = activity.getLayoutInflater();
        sResponseLayoutAlertDialog = new AlertDialog.Builder(activity).create();
        sResponseLayoutAlertDialog.setCancelable(false);
        if (sResponseLayoutAlertDialog != null) {
            sResponseLayoutAlertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        sResponseLayoutAlertDialogView = sResponseLayoutInflater.inflate(R.layout.activity_response_alert_layout, null);
        sResponseLayoutAlertDialog.setView(sResponseLayoutAlertDialogView);

        sButtonResponseALert=(Button)sResponseLayoutAlertDialogView.findViewById(R.id.response_alert_button);
        sTextViewResponseAlert=(TextView)sResponseLayoutAlertDialogView.findViewById(R.id.response_alert_textview);
        sProgressBar=(ProgressBar)sResponseLayoutAlertDialogView.findViewById(R.id.response_alert_progress_bar);

        sTextViewResponseAlert.setText(alerttext);
        sButtonResponseALert.setText(buttonText);

        sButtonResponseALert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(sResponseLayoutAlertDialog!=null && sResponseLayoutAlertDialog.isShowing())
                {
                    sResponseLayoutAlertDialog.dismiss();
                }
            }
        });




        if(sResponseLayoutAlertDialog!=null && sResponseLayoutAlertDialog.isShowing())
        {
            sResponseLayoutAlertDialog.dismiss();
        }

        if(progressBarStatus)
        {
            sProgressBar.setVisibility(View.VISIBLE);
        }else
        {
            sProgressBar.setVisibility(View.GONE);
        }
        sResponseLayoutAlertDialog.show();
    }

}
