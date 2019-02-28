package com.tempreader.temp.temp;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TempController {
    @Autowired
    TempService tempservice;

    @GetMapping("/api/temps")
    @ApiOperation(value = "Show all Temps", response = Temp.class)
    @ApiResponses(value = {
     @ApiResponse(code = 200, message = "Server reachable")})
    public List<Temp> getTemps(){
        return tempservice.getTemps();
    }

    @GetMapping("api/temps/{id}")
    @ApiOperation(value="Show 1 Temp", response = Temp.class)
    public Temp getTemp(@PathVariable int id ){
        return tempservice.getTemp(id);
    }

    @PutMapping(value = "/api/temps/{id}", consumes = "application/json", produces = "application/json")
    public void updateTemp(@RequestBody Temp temp){
        tempservice.updateTemp(temp);
    }

    @PostMapping(value = "api/temps/add",consumes = "application/json", produces = "application/json")
    public void addTemp(@RequestBody Temp temp){
        tempservice.addTemp(temp);
    }

}
