package enums;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;


interface RoundFunc {

    public float operation(float a);
}

public enum RoundType  {

    UPPER_DECIMAL( x -> {
        DecimalFormat df = new DecimalFormat("#");
        df.setRoundingMode(RoundingMode.CEILING);
        return Float.parseFloat(df.format(x));
    })
   ,UPPER_TEN_DECIMAL( x -> {
        DecimalFormat df = new DecimalFormat("#");
        df.setRoundingMode(RoundingMode.CEILING);
        return Float.parseFloat(df.format(x / 10)) * 10;
    });

    private RoundFunc roundFunc;

    public static ArrayList<RoundType> list = new ArrayList<>(Arrays.asList(
        UPPER_DECIMAL
       ,UPPER_TEN_DECIMAL
    ));


    RoundType( RoundFunc roundFunc){
        this.roundFunc = roundFunc;
    }

    public static RoundType exists(String enumStr){

        if(list.stream().anyMatch(x -> x.toString().equals(enumStr))){
            return list.stream().filter( x -> x.toString().equals(enumStr)).findAny().get();
        }
        return null;
    }

    public float round(float number){
        return roundFunc.operation(number);
    }
}
