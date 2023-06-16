/*
 * Copyright (C) 2023 jtalbut
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package uk.co.spudsoft.query.main;

import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.CLASS;
import java.lang.annotation.Target;

/**
 * JaCoCo will ignore any method annotated with this.
 * 
 * Classes and methods annotated by annotation whose retention policy is runtime 
 * or class and whose simple name contains "Generated" are filtered out during 
 * generation of JaCoCo report (GitHub #822).
 * 
 * @author jtalbut
 */
@Documented
@Retention(CLASS)
@Target({TYPE, METHOD})
public @interface ExcludeFromJacocoGenerated {
  
}
