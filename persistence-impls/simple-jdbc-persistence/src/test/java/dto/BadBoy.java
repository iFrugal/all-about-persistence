package dto;

import lazydevs.mapper.db.jdbc.JDBCParam.Type;
import lazydevs.mapper.db.jdbc.annotation.Column;
import lazydevs.mapper.db.jdbc.annotation.Entity;
import lazydevs.mapper.db.jdbc.annotation.Id;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@Getter @EqualsAndHashCode @ToString
public class BadBoy implements Serializable {

    @Column(dbType = Type.STRING)
    private String name;

    @Id @Column(dbType = Type.SHORT)
    private short rollNo;

    public BadBoy(String name, int rollNo) {
        this.name = name;
        this.rollNo = (short)rollNo;
    }

    public BadBoy(){

    }

}
