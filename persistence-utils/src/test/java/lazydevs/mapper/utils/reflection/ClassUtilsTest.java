package lazydevs.mapper.utils.reflection;

import lazydevs.mapper.utils.reflection.ClassUtils.Arg;
import lazydevs.mapper.utils.reflection.ClassUtils.NamedArg;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collection;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class ClassUtilsTest {


    @Test
    public void testGet() throws Exception {
        Collection c = ClassUtils.getInterfaceRef("java.util.LinkedHashSet", Collection.class, new Arg[]{new Arg<>(5, int.class)});
        c.add(getI(1, "Abhijeet Rai", 8, "A"));
        c.add(getI(2, "Abhishek Rai", 10, "A"));
        int counter = 1;
        for(Object e : c){
            System.out.println(e);
            assertTrue(e instanceof I);
            I i = (I) e;
            assertEquals(i.getRollNo(), counter++);
            assertEquals(i.getCountry(), "US");
            assertEquals(i.getUniqueCitizenshipId(), "ALIPPPRR3344");
        }
    }

    @Test
    public void testLoadClass(){
        Assert.assertEquals(ClassUtils.loadClass("java.lang.String[]"), String[].class);
        Assert.assertEquals(ClassUtils.loadClass("[Ljava.lang.String;"), String[].class);
        Assert.assertEquals(ClassUtils.loadClass("[Ljava.lang.Integer;"), Integer[].class);
        Assert.assertEquals(ClassUtils.loadClass("int[]"), int[].class);
        Assert.assertEquals(ClassUtils.loadClass("[I"), int[].class);
    }


    private static I getI(int rollNo, String name, int classI, String section){
        return ClassUtils.getInterfaceRef(A.class.getName(), I.class, new Arg[]{new Arg<>(rollNo, int.class), new Arg<>(name, String.class)}
                , new NamedArg[]{
                        new NamedArg("classI", classI)
                        , new NamedArg("section", section)
                        , new NamedArg("country", "US")
                },
                new ClassUtils.NamedMethod("setUniqueCitizenshipId", "ALIPPPRR3344", String.class)
        );
    }

    interface I{
        int getRollNo();
        String getUniqueCitizenshipId();
        String getCountry();
    }

    @RequiredArgsConstructor
    @ToString @Getter
    static class A extends Parent{
        private final int rollNo;
        private final String name;
        @Setter
        private int classI;
        @Setter private String section;
    }

    @RequiredArgsConstructor
    @ToString @Getter
    static abstract class Parent implements I{
        @Setter private String uniqueCitizenshipId;
        public String country;

    }
}