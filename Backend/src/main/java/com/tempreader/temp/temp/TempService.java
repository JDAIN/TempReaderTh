package com.tempreader.temp.temp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TempService {

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

    public void addTemp(Temp temp) {
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

//        s = Pattern.matches( "\\.\\d{2}\\s", "Hallo Welt");
        return retTempTest;
    }


    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Gets Temps in Temps from last temp -hours entered.
     * ex: Temps of last Hours: hours=1 last30Days =720
     *
     * @param hours hours for offset
     * @return List of Temps
     */
    public List<Temp> getTempsByLastHours(int hours) {
        //TODO test
        DateTimeFormatter tempFormat = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm:ss");
//        Temp lTemp = tempRepository.findFirstByOrderByIdDesc();
        Temp lTemp = tempRepository.findAllByOrderByIdDesc().get(0);
        List<Temp> test = tempRepository.findAllByOrderByIdDesc();

        LocalDateTime start = LocalDateTime.parse(lTemp.getDate(), tempFormat);


        List<Temp> result = test.stream()
                .filter(t -> ChronoUnit.HOURS.between(start, LocalDateTime.parse(t.getDate(), tempFormat)) <= -hours)
                .collect(Collectors.toList());

        result.forEach(System.out::println);
        return result;
    }

    public double getAverageTempInDurationHours(int hours) {
        List<Temp> TempsListInHour = getTempsByLastHours(hours);
        double sum = TempsListInHour.stream()
                .mapToDouble(tempVal -> tempVal.getTemperature())
                .sum();
        //TODO test
        return sum / TempsListInHour.size();
    }

    public double getAverageHumidityInDurationHours(int hours) {
        //TODO test
        List<Temp> TempsListInHour = getTempsByLastHours(hours);
        double sum = TempsListInHour.stream()
                .mapToDouble(tempVal -> tempVal.getHumidity())
                .sum();

        return sum / TempsListInHour.size();
    }


}