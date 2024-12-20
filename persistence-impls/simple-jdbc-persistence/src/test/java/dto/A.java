package dto;

import lazydevs.mapper.db.jdbc.JDBCParam.Type;
import lazydevs.mapper.db.jdbc.annotation.Column;
import lazydevs.mapper.db.jdbc.annotation.Entity;
import lazydevs.mapper.db.jdbc.annotation.Id;
import lombok.*;

import java.io.Serializable;

@Entity(value = "A_TABLE", autoCreate = true, defaultOrder = "rollNo") @Getter @EqualsAndHashCode @ToString
public class A implements Serializable {

    @Column(dbType = Type.STRING)
    private String name;

    @Id @Column(dbType = Type.SHORT)
    private short rollNo;

    public A(String name, int rollNo) {
        this.name = name;
        this.rollNo = (short)rollNo;
    }

    public A(){

    }

}
