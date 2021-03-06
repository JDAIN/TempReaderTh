package com.tempreader.temp.temp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;


import java.util.ArrayList;
import java.util.List;

@Service
public class TempService {
    //logger sl4j
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private TempRepository tempRepository;

    public List<Temp> getTemps() {
        return tempRepository.findAll();
    }

    public Temp getTemp(long searchId) {
        return tempRepository.findById(searchId);
    }

    public void updateTemp(Temp temp) {
        tempRepository.save(temp);
    }

    public void addTemp(Temp temp) throws TempMissesInfoException, DateMissmatchException {

        if (temp.getHumidity() == 0 || temp.getTemperature() == 0 || (temp.getDate().isEmpty() || temp.getDate() == null)) {
            throw new TempMissesInfoException();
        }
        if (!temp.getDate().matches("^\\d{2}.\\d{2}.\\d{2}\\s\\d{2}:\\d{2}:\\d{2}$")) {
            throw new DateMissmatchException();
        }
        temp.setId(0); //if id is sent, with post, remove it and use incremented id
        tempRepository.save(temp);
    }

    public List<Temp> getTempsByMonthAndYear(String Month, String Year) {
        String searchTerm = String.format(".%s.%s", Month, Year);
        return tempRepository.findAllByDateContainingIgnoreCase(searchTerm);


    }

    public List<Temp> getTempsByDayMonthAndYear(String day, String Month, String Year) {
        String searchTerm = String.format("%s.%s.%s", day, Month, Year);
        return tempRepository.findAllByDateContainingIgnoreCase(searchTerm);


    }

    public List<Temp> getTempsByYear(String year) {
        String searchTerm = String.format(".%s ", year);
        return tempRepository.findAllByDateContainingIgnoreCase(searchTerm);
    }

    public List<Temp> getTempsList(String day, String month, String year) {
        //TODO improve
        if (isNullOrEmpty(day) && isNullOrEmpty(month)) {
            return tempRepository.findAllByDateContainingIgnoreCase(String.format(".%s ", year));

        } else if (isNullOrEmpty(day) && !isNullOrEmpty(month)) {
            return tempRepository.findAllByDateContainingIgnoreCase(String.format(".%s.%s", month, year));

        } else return tempRepository.findAllByDateContainingIgnoreCase(String.format("%s.%s.%s ", day, month, year));
    }

    public List<Temp> getLast50Temps(){
        return tempRepository.findTop50ByOrderByIdDesc();
    }

    /**
     * Creates Temp in Database, used for Test. Data does not get checked.
     *
     * @param temp
     */
    public void createTemp(Temp temp) {
        tempRepository.save(temp);
    }


    public Temp getLastTempEntry() {
        //TODO; @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
        // If I use retTemp and edit it it gets saved to the database and readOnly didnt work


        Temp retTemp = tempRepository.findFirstByOrderByIdDesc();
        Temp retTempTest = new Temp();
        retTempTest.setDate(retTemp.getDate());
        retTempTest.setId(retTemp.getId());
        retTempTest.setHumidity(retTemp.getHumidity());
        retTempTest.setTemperature(retTemp.getTemperature());

        String s = retTempTest.getDate();
//        Pattern pattern = Pattern.compile("\\.\\d{2}\\s");
        //TODO maybe replace with stringutils for better performance?
        s = s.replaceAll("\\.\\d{2}\\s", ". ");
        s = s.replaceAll("\\:\\d{2}$", "");
        retTempTest.setDate(s);

        return retTempTest;
    }


    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Gets Temps in Temps from last temp - hours entered.
     * ex: Temps of last Hours: hours= 1 last30Days = 720
     * Stops after first false, doesnt work if temps are out of order.
     * Uses estimated Value amount (1 Value = 1 Min) for DB optimization!
     *
     * @param hours hours for offset
     * @return List of Temps
     */
    public List<Temp> getTempsByLastHours(int hours) {
        //TODO: if there is not enough hours between start and offset(hours) it will not display anything BUG
        DateTimeFormatter tempFormat = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss");
        int estimatedAmountOfValuesInHours = hours * 60 + 10; //1 Temp every Minute + buffer
        System.out.println(estimatedAmountOfValuesInHours);
        List<Temp> tempList = tempRepository.findTempsByAmount(estimatedAmountOfValuesInHours);

        Temp lTemp = tempList.get(tempList.size()-1); //last element
        LocalDateTime start = LocalDateTime.parse(lTemp.getDate(), tempFormat);
        List<Temp> retList = new ArrayList<>();
        for (Temp t : tempList) {
            if (ChronoUnit.HOURS.between(start, LocalDateTime.parse(t.getDate(), tempFormat)) >= -hours) {
                retList.add(t);
            } else break; //stops after first wrong, doesnt work if temps are out of order
        }
        return retList;
    }

    /**
     * Returns a String Array with avg Temps and Humidity rounded to 2 decimals
     * Value 0 in Array is the avg Temp, Value 1 in Array is the avg Humidity
     * @param hours
     * @return String Array with avg Temps and Humidity
     */
    @Cacheable(value = "averageTemps", condition = "#hours>1", key = "#root.args[0]", sync = true)
    public String[] getAverageTempAndHumidityInDurationHours(int hours) {
        List<Temp> TempsListInHour = getTempsByLastHours(hours);
        //gets Temp
        double tempSum = TempsListInHour.stream()
                .mapToDouble(tempVal -> tempVal.getTemperature())
                .sum();
        //gets humidity
        double humiditySum = TempsListInHour.stream()
                .mapToDouble(tempVal -> tempVal.getHumidity())
                .sum();

        return new String[]{String.format("%.2f", tempSum / TempsListInHour.size()),
                String.format("%.2f", humiditySum / TempsListInHour.size())};
    }

    @CachePut(value = "averageTemps", key = "#root.args[0]")
    public String[] updateGetAverageTempAndHumidityInDurationHours(int hours) {
        List<Temp> TempsListInHour = getTempsByLastHours(hours);
        //gets Temp
        double tempSum = TempsListInHour.stream()
                .mapToDouble(tempVal -> tempVal.getTemperature())
                .sum();
        //gets humidity
        double humiditySum = TempsListInHour.stream()
                .mapToDouble(tempVal -> tempVal.getHumidity())
                .sum();

        return new String[]{String.format("%.2f", tempSum / TempsListInHour.size()),
                String.format("%.2f", humiditySum / TempsListInHour.size())};
    }

    public List<Temp> getTempsByAmount(int amount){
        return tempRepository.findTempsByAmount(amount);
    }
}
