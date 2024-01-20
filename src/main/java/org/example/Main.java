package org.example;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.FileReader;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main {
    public static void main(String[] args)
    {

        String filePath = "F:/bluejay internship/employeeData.csv";
        CSVReader reader=null;
        try
        {
            // Query1: who has worked for 7 consecutive days
            reader = new CSVReader(new FileReader(filePath));
            System.out.println("Query1: who has worked for 7 consecutive days\n");
            query1(reader);
            System.out.println("\n\n");
            reader.close();


            //Query2: Who have less than 10 hours of time between shifts but greater than 1 hour
            reader = new CSVReader(new FileReader(filePath));
            System.out.println("Query2: Who have less than 10 hours of time between shifts but greater than 1 hour\n");
            query2(reader);
            System.out.println("\n\n");
            reader.close();

            //Query3: Who has worked for more than 14 hours in a single shift
            reader = new CSVReader(new FileReader(filePath));
            System.out.println("Query3: Who has worked for more than 14 hours in a single shift\n");
            query3(reader);
            System.out.println("\n\n");
            reader.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        //catch for error in readnext
        catch (CsvValidationException e)
        {
            throw new RuntimeException(e);
        }

    }
    private static void query3(CSVReader reader) throws CsvValidationException, IOException
    {
        // map to store employee names with their maximum hours in all shifts
        Map<String,Long> mapHours = new HashMap<>();

        //map to store employee names with their position ID
        Map<String,String> mapPosition = new HashMap<>();

        String[] nextLine= reader.readNext();
        while(nextLine!=null)
        {
            nextLine= reader.readNext();
            if(nextLine==null)                      // if a row is null then skip it
                continue;

            String name = nextLine[7];              // "Employee Name" is in the 8th column
            String position = nextLine[0];          // "Position ID" is in the 1st column
            String stringTimeIn = nextLine[2];      //  "Time in" is in the 3rd column
            String stringTimeOut = nextLine[3];     //  "Time out" is in the 4th column
            
            long timeInTimestamp = getTimestamp(stringTimeIn);      //convert time in of everyday into timestamp
            long timeOutTimestamp = getTimestamp(stringTimeOut);    //convert time out of everyday into timestamp

            Duration durationWorked = getDuration(timeInTimestamp,timeOutTimestamp);  //get duration between time in and time out in a single shift
            long hoursWorked = getHoursFromDuration(durationWorked);    //get number of hours from the duration of a single shift

            if(mapHours.containsKey(name))  // if the employee has already been added then key only the max number of hours worked in a single shift
            {
                mapHours.put(name,Math.max(mapHours.get(name),hoursWorked));
            }
            else
            {
                mapHours.put(name,hoursWorked); // store no. of hours worked by new employee in a single shift
                mapPosition.put(name,position); //store position ID of new employee
            }
        }
        for(Map.Entry<String,Long> entry : mapHours.entrySet()) // for every employee check their max number of hours worked
        {
            String name = entry.getKey();

            if(entry.getValue()>=14)    //print name and position of employee only if max hours worked in a single shift is greater than 14
            {

                printNameAndPosition(name, mapPosition.get(name));
            }
        }
    }
    private static void query2(CSVReader reader) throws CsvValidationException, IOException
    {
        // map to store employee names with their all shifts having time in and time out of each shift
        Map<String,ArrayList<Shift>> mapShifts = new HashMap<>();

        //map to store employee names with their position ID
        Map<String,String> mapPosition = new HashMap<>();
        String[] nextLine= reader.readNext();
        while(nextLine!=null)
        {
            nextLine = reader.readNext();
            if (nextLine == null)                      // if a row is null then skip it
                continue;

            String name = nextLine[7];              // "Employee Name" is in the 8th column
            String position = nextLine[0];          // "Position ID" is in the 1st column
            String stringTimeIn = nextLine[2];      //  "Time in" is in the 3rd column
            String stringTimeOut = nextLine[3];     //  "Time out" is in the 4th column

            long timeInTimestamp = getTimestamp(stringTimeIn);      //convert time in of everyday into timestamp
            long timeOutTimestamp = getTimestamp(stringTimeOut);    //convert time out of everyday into timestamp

            ArrayList<Shift> shifts;    //to store all shifts of each employee
            if(mapShifts.containsKey(name))
            {
                shifts = mapShifts.get(name);
            }
            else
            {
                shifts = new ArrayList<>();
                mapPosition.put(name,position); // to store position of each employee
            }
            shifts.add(new Shift(timeInTimestamp,timeOutTimestamp));
            mapShifts.put(name,shifts);
        }


        for(Map.Entry<String,ArrayList<Shift>> entry: mapShifts.entrySet()) //iterate over each employee to find gap between all shifts
        {
            String name = entry.getKey();

            ArrayList<Shift> shifts = entry.getValue();

            shifts.sort(Comparator.comparingLong(Shift::getTimeIn));    // Sort shifts by timeInTimestamp to ensure sequential order

            for(int i=0;i<shifts.size()-1;i++)
            {
                long todayTimeOut = shifts.get(i).timeOut;  //timestamp of time out of current day
                long tomorrowTimeIn = shifts.get(i+1).timeIn;   //timestamp of time in of next day

                Duration gapDuration = getDuration(todayTimeOut,tomorrowTimeIn);    // duration between the two

                long gapHours = getHoursFromDuration(gapDuration);  // extract no. of hours from duration
                if(gapHours>=1 && gapHours<10)  // print only if no. of hours greater than 1 and less than 10
                    printNameAndPosition(name,mapPosition.get(name));
            }

        }
    }
    private static void query1(CSVReader reader) throws CsvValidationException, IOException
    {

        // map to store all days they work for each employee
        Map<String, SortedSet<Integer>> mapDays = new HashMap<>();

        //map to store employee names with their position ID
        Map<String,String> mapPosition = new HashMap<>();

        // Iterate through rows
        String[] nextLine= reader.readNext();
        while(nextLine!=null)
        {
            nextLine = reader.readNext();
            if(nextLine==null)  // skip row if it is empty
                continue;

            String name = nextLine[7];          // "Employee Name" is in the 8th column
            String position = nextLine[0];      // "Position ID" is in the 1st column
            String dateCell = nextLine[2];      //  "Time in" is in the 3rd column

            if(dateCell.length()==0)    // if the date-time cell do not contain date-time
                continue;
            int day = Integer.parseInt(dateCell.substring(3,5));    // extract the day of the month on which employee came

            SortedSet<Integer> l;                               // to store all working days in sorted order
            if(mapDays.containsKey(name))
            {
                l = mapDays.get(name);
            }
            else
            {
                l = new TreeSet<>();
                mapPosition.put(name,position);
            }

            l.add(day);
            mapDays.put(name,l);

        }
        countMaxConsecutiveDays(mapDays,mapPosition);    // to count the maximum consecutive days worked and print those who worked for 7 consecutive days
    }
    private static long getHoursFromDuration(Duration duration)
    {
        return duration.toHours(); //extract number of hours from time duration
    }
    private static Duration getDuration(long timeInTimestamp,long timeOutTimestamp)
    {
        Instant instant1 = Instant.ofEpochMilli(timeInTimestamp);
        Instant instant2 = Instant.ofEpochMilli(timeOutTimestamp);

        return Duration.between(instant1, instant2);    // Calculate the duration between two instants
    }
    private static long getTimestamp(String time)
    {
        DateTimeFormatter formatter = null;
        boolean parsingDone=false;  //flag to check if the date-time has be parsed or not
        if(tryParsingType1(time))   // check if the date-time is in the format MM/dd/yyyy hh:mm a eg. 09/12/2023 10:40 PM
        {
            formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a", Locale.ENGLISH);
            parsingDone=true;
        }
        else if(tryParsingType2(time))  // check if the date-time is in the format MM/dd/yyyy hh:mm a eg. 09-12-2023 22:40
        {
            formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm", Locale.ENGLISH);
            parsingDone=true;
        }
        if(parsingDone) //if the given date-time is parsed in anyone of the two formats then calculate its timestamp
        {
            LocalDateTime localDateTime = LocalDateTime.parse(time, formatter);
            long numericTimestamp = localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
            return numericTimestamp;
        }
        return 0;   // if not parsed , i.e, some wrong date-time format or empty
    }
    private static boolean tryParsingType1(String time)
    {
        try{
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm a", Locale.ENGLISH);
            LocalDateTime localDateTime = LocalDateTime.parse(time, formatter);
            return true;
        }
        catch (DateTimeParseException e) {
            return false;
        }
    }
    private static boolean tryParsingType2(String time)
    {
        try{
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm", Locale.ENGLISH);
            LocalDateTime localDateTime = LocalDateTime.parse(time, formatter);
            return true;
        }
        catch (DateTimeParseException e) {
            return false;
        }
    }
    private static void countMaxConsecutiveDays(Map<String,SortedSet<Integer>> map,Map<String,String> mapPosition)
    {
        for(Map.Entry<String,SortedSet<Integer>> employee: map.entrySet()) //iterate over each employee
        {
            SortedSet<Integer> days = employee.getValue();
            if(days.size()<7)          // if a user has worked for less than 7 days then consecutive 7 days not possible
                continue;
            Iterator<Integer> i = days.iterator();
            int max=0;                  // to store max number of consecutive days
            int prevday=i.next();
            int count=0;                // to count current number of consecutive days

            while(i.hasNext())
            {
                int curr=i.next();       // to store current day
                if(prevday+1<curr) {        // if the days are not consecutive
                    max = Math.max(max, ++count);       //put maximum in max
                    count = 0;                      // initialize the current count as 0 as a new series of consecutive days start

                }
                else
                {
                    count++;
                }
                prevday=curr;
            }
            max = Math.max(max, ++count);
            String position=mapPosition.get(employee.getKey());
            if(max>=7)
                printNameAndPosition(employee.getKey(),position);
        }
    }
    private static void printNameAndPosition(String name,String position)
    {
            System.out.println("Name: " + name+"\t "+"Position ID: "+position);
    }
}

