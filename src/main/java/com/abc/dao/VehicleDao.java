package com.abc.dao;

import com.abc.domian.Vehicle;
import it.unisa.dia.gas.jpbc.Element;
import org.apache.ibatis.annotations.*;

import java.math.BigInteger;
import java.util.List;


public interface VehicleDao {

    @Select("select * from vehicle where name=#{name}}")
    Vehicle selectAll(@Param("name")String name);

    @Insert("insert into vehicle(name,pk1,pk2,sk1,sk2) values (#{name},#{pk1},#{pk2},#{sk1},#{sk2})")
    int  insertAll(@Param("name") String name, @Param("pk1")String pk1, @Param("pk2")String pk2, @Param("sk1")String sk1, @Param("sk2")String sk2);

}
