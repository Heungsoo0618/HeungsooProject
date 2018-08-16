package com.elkane.heungsootest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends Activity implements View.OnClickListener {
    RelativeLayout rel_bottomPopup;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_numberpicker);
        Util.logcat("액티비티 생성.");
        startService(new Intent(this, BlueLinkBLEService.class));
//        TimePicker timePicker = (TimePicker)findViewById(R.id.timePicker);
//        rel_bottomPopup = (RelativeLayout)findViewById(R.id.rel_bottomPopup);
//        EditText edt_test =  (EditText)findViewById(R.id.edt_test);
//        edt_test.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(rel_bottomPopup.getVisibility()==View.GONE)
//                    rel_bottomPopup.setVisibility(View.VISIBLE);
//            }
//        });
//        CalendarView calendar = findViewById(R.id.calendar);
//        initialize();
        initializeTimePicker();
    }
    private void heungsooLog(String msg)
    {
        Log.d("heungsoo",msg);
    }

    ScrollView scrollview,scrollview_hour;
    ArrayList<TextView> minutesList,hoursList;
//    int currentScrollPosition;

    private Long lastScrollUpdate_minutes = -1l;
    private Long lastScrollUpdate_hours = -1l;
    private MinutesScrollStateHandler minutesHandler;
    private HoursScrollStateHandler hoursHandler;
    private int selectedHours,selectedMinutes;
    TextView txt_Time;
    private class HoursScrollStateHandler implements Runnable {

        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            int currentScrollPosition = scrollview_hour.getScrollY();
            if ((currentTime - lastScrollUpdate_hours) > 100) {
                lastScrollUpdate_hours = -1l;
                heungsooLog("시단위 스크롤 멈춘겨 : " + (float)currentScrollPosition/200 + ", 반올림값 : " + Math.round((float)currentScrollPosition/200) + ", 스크롤뷰 : " + scrollview_hour + ", this.hashCode() : " + this.hashCode());
                scrollview_hour.smoothScrollTo(0,Math.round((float)currentScrollPosition/200)*200);
                if(currentScrollPosition%200==0)
                {
                    //정확하게 멈췄을 때
                    int currentPosition = currentScrollPosition/200;
                    for(TextView item : hoursList)
                    {
                        item.setTextColor(0x994a4a50);
                    }
                    hoursList.get(currentPosition).setTextColor(0x994a4a50);
                    hoursList.get(currentPosition+1).setTextColor(0xff4a4a50);
                    hoursList.get(currentPosition+2).setTextColor(0xff4ad3dd);
                    hoursList.get(currentPosition+3).setTextColor(0xff4a4a50);
                    hoursList.get(currentPosition+4).setTextColor(0x994a4a50);
//                    selectedHours = hoursList.get(currentPosition+2).getText().toString();
                    selectedHours = currentPosition;
                    printPickedTime();
                }
            } else {
                scrollview_hour.postDelayed(this, 100);
            }
        }
    }


    private class MinutesScrollStateHandler implements Runnable {

        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            int currentScrollPosition = scrollview.getScrollY();
            if ((currentTime - lastScrollUpdate_minutes) > 100) {
                lastScrollUpdate_minutes = -1l;
                heungsooLog("분단위 스크롤 멈춘겨 : " + (float)currentScrollPosition/200 + ", 반올림값 : " + Math.round((float)currentScrollPosition/200) + ", 스크롤뷰 : " + scrollview + ", this.hashCode() : " + this.hashCode());
                scrollview.smoothScrollTo(0,Math.round((float)currentScrollPosition/200)*200);
                if(currentScrollPosition%200==0)
                {
                    //정확하게 멈췄을 때
                    int currentPosition = currentScrollPosition/200;
                    for(TextView item : minutesList)
                    {
                        item.setTextColor(0x994a4a50);
                    }
                    minutesList.get(currentPosition).setTextColor(0x994a4a50);
                    minutesList.get(currentPosition+1).setTextColor(0xff4a4a50);
                    minutesList.get(currentPosition+2).setTextColor(0xff4ad3dd);
                    minutesList.get(currentPosition+3).setTextColor(0xff4a4a50);
                    minutesList.get(currentPosition+4).setTextColor(0x994a4a50);
//                    selectedMinutes = minutesList.get(currentPosition+2).getText().toString();
                    minutesList.get(currentPosition+2).setTypeface(minutesList.get(currentPosition+2).getTypeface(), Typeface.BOLD);
                    selectedMinutes = currentPosition;
                    printPickedTime();
                }
            } else {
                scrollview.postDelayed(this, 100);
            }
        }
    }

    private void printPickedTime()
    {
        txt_Time.setText("선택된 시간 : "+selectedHours +  " 시 " + selectedMinutes + "분");
        TextView txtAmPM = findViewById(R.id.txtAmPM);
        txtAmPM.setText(selectedHours<12?"AM":"PM");
    }


    private void initializeTimePicker()
    {
        txt_Time = findViewById(R.id.txt_Time);
        LinearLayout lin_scroll = findViewById(R.id.lin_scroll);
        LinearLayout lin_scroll_hour = findViewById(R.id.lin_scroll_hour);
        hoursList = new ArrayList<>();
        for(int i=-2;i<26;i++)
        {
            TextView textView = new TextView(this);
            if(i>=0&&i<24)
                textView.setText(i+"");
            textView.setTextSize(20f);
            textView.setGravity(Gravity.CENTER );
            lin_scroll_hour.addView(textView,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,200));
            hoursList.add(textView);
        }


        minutesList = new ArrayList<>();
        for(int i=-2;i<62;i++)
        {
            TextView textView = new TextView(this);
            if(i>=0&&i<60)
                textView.setText(i+"");
            textView.setTextSize(20f);
            textView.setGravity(Gravity.CENTER );
            lin_scroll.addView(textView,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,200));
            minutesList.add(textView);
        }

        scrollview = findViewById(R.id.scrollview);
        scrollview_hour = findViewById(R.id.scrollview_hour);
        minutesHandler = new MinutesScrollStateHandler();
        hoursHandler = new HoursScrollStateHandler();

        scrollview.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
//                heungsooLog("분단위 스크롤 " + scrollview.getScrollY());
//                currentScrollPosition = scrollview.getScrollY();
                if (lastScrollUpdate_minutes == -1)
                {

                    scrollview.postDelayed(minutesHandler, 100);
                }
                lastScrollUpdate_minutes = System.currentTimeMillis();
            }
        });
        scrollview_hour.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
//                heungsooLog("시간단위 스크롤" +scrollview_hour.getScrollY() );
//                currentScrollPosition = scrollview_hour.getScrollY();
                if (lastScrollUpdate_hours == -1)
                {
                    scrollview_hour.postDelayed(hoursHandler, 100);
                }
                lastScrollUpdate_hours = System.currentTimeMillis();
            }
        });




//        lst_hour.setOnScrollListener(new AbsListView.OnScrollListener() {
//            @Override
//            public void onScrollStateChanged(AbsListView view, int scrollState) {
//                if(scrollState ==SCROLL_STATE_IDLE)
//                {
////                    heungsooLog("스크롤 멈춤, mFirstVisibleItem :" + mFirstVisibleItem + " ,  scrollYY : " + scrollYY);
//                    if(mFirstVisibleItem<8)
//                    {
//                        new Handler().postDelayed(new Runnable() {
//                            @Override
//                            public void run() {
////                                lst_hour.setSelection(mFirstVisibleItem);
////                                lst_hour.smoothScrollToPosition(mFirstVisibleItem);
//                            }
//                        },500);
//                    }
//
//                }
//            }
//
//            @Override
//            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
////                heungsooLog("firstVisibleItem : " + firstVisibleItem + " ,  visibleItemCount : " + visibleItemCount);
////                TextView textview = ((View)(adapter.getItem(firstVisibleItem))).findViewById(R.id.calendar_cell_tv);
////                textview.setTextColor(getResources().getColor(R.color.colorAccent));
//             if(view.getChildAt(firstVisibleItem)!=null)
//                 heungsooLog("visibleItemCount : " + visibleItemCount + " , adapter.getItem : " + adapter.getItem(firstVisibleItem) + " 의 getY : " + view.getChildAt(firstVisibleItem).getY());
//                mFirstVisibleItem = firstVisibleItem;
//            }
//        });


//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try
//                {
//                    Thread.sleep(1000);
//                }
//                catch (Exception e)
//                {
//                    e.printStackTrace();
//                }
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        scrollview.smoothScrollTo(0,2400);
//                    }
//                });
//            }
//        }).start();



    }


    @Override
    protected void onDestroy() {

        super.onDestroy();
        stopService(new Intent(this, BlueLinkBLEService.class));
    }

    String[] mDays;
    private class numberPickerAdapter extends BaseAdapter {
        private LayoutInflater inflater;

        public class CalendarCell {
            public TextView tv;
        }

        public numberPickerAdapter(Context context) {
            inflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return mDays.length;
        }

        @Override
        public Object getItem(int position) {
            return mDays[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            CalendarCell cell;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.cell_label, null);
                cell = new CalendarCell();
                cell.tv = convertView.findViewById(R.id.calendar_cell_tv);
            } else {
                cell = (CalendarCell) convertView.getTag();
            }
//            heungsooLog("현재 포지션 : " + position + " ,  firstVisibleItem : " + mFirstVisibleItem);
            String day = mDays[position];
            cell.tv.setText(day);
            convertView.setTag(cell);
            return convertView;
        }
    }




    CustomCalendarView calendarView;
    private void initialize()
    {
        calendarView = (CustomCalendarView) findViewById(R.id.calendarview);
        Button btnPrev = (Button) findViewById(R.id.btnPrev);
        final Button btnDate = (Button) findViewById(R.id.btnDate);
        Button btnNext = (Button) findViewById(R.id.btnNext);
        btnPrev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                calendarView.showLastsMonth();
                Calendar calendar = calendarView.getCalendar();
                btnDate.setText(getDateString(calendar));
            }
        });
        btnNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                calendarView.showNextMonth();
                btnDate.setText(getDateString(calendarView.getCalendar()));
            }
        });
        calendarView.setOnDateSelectedListener(new CustomCalendarView.OnDateSelectedListener() {
            @Override
            public void onDateSelected(Calendar calendar) {
                Log.d("heungsoo","선택된 날 : " + getDateString(calendar));
            }
        });

        Calendar calendar = Calendar.getInstance();
        Calendar minCalendar = Calendar.getInstance();
        minCalendar.add(Calendar.MONTH, -2);
        Calendar maxCalendar = Calendar.getInstance();
        maxCalendar.add(Calendar.MONTH, 2);

//        calendarView.setMinMaxCalendar(minCalendar, maxCalendar);
        calendarView.setCalendar(calendar);
        // calendarView.setShowLines(false);
        calendarView.setLineColor(Color.DKGRAY);
        calendarView.setLineWidth(1);
        // calendarView.hideWeekLabel();
        calendarView.setbgColorOfToday(Color.YELLOW);
    }
    private String getDateString(Calendar calendar) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
        return format.format(calendar.getTime());
    }


    public void onClickEdit(View view)
    {
        rel_bottomPopup.setVisibility(View.GONE);
    }

    @Override
    public void onBackPressed() {
//        rel_bottomPopup.setVisibility(View.GONE);
        super.onBackPressed();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(newConfig.orientation== Configuration.ORIENTATION_PORTRAIT)
        {
            Log.d("heungsoo","세로유");
        }
        else
            Log.d("heungsoo","가로유~");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId())
        {
//            case R.id.timePicker:
//                break;
        }
    }
}
