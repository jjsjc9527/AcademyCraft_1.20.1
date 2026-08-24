package cn.lambdalib2.datapart;

import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.LogicalSide;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegDataPart {

    Class<? extends Entity> value();

    LogicalSide[] side() default { LogicalSide.CLIENT, LogicalSide.SERVER };

}
