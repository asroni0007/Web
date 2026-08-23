package com.abc.dao;

import com.abc.domian.Vehicle;
import org.apache.ibatis.annotations.*;


public interface VehicleDao {

    @Select("SELECT * FROM vehicle WHERE name = #{name}")
    Vehicle selectByName(@Param("name") String name);

    @Insert("insert into vehicle(name,pk1,pk2,sk1,sk2) values (#{name},#{pk1},#{pk2},#{sk1},#{sk2})")
    int insertAll(@Param("name") String name,
                  @Param("pk1") String pk1,
                  @Param("pk2") String pk2,
                  @Param("sk1") String sk1,
                  @Param("sk2") String sk2);

}
