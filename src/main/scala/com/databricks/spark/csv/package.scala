/*
 * Copyright 2014 Databricks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.databricks.spark

import org.apache.spark.sql.{SQLContext, DataFrame}

package object csv {

  /**
   * Adds a method, `csvFile`, to SQLContext that allows reading CSV data.
   */
  implicit class CsvContext(sqlContext: SQLContext) {
    def csvFile(filePath: String) = {
      val csvRelation = CsvRelation(
        location = filePath,
        useHeader = true,
        delimiter = ',',
        quote = '"')(sqlContext)
      sqlContext.baseRelationToDataFrame(csvRelation)
    }

    def tsvFile(filePath: String) = {
      val csvRelation = CsvRelation(
        location = filePath,
        useHeader = true,
        delimiter = '\t',
        quote = '"')(sqlContext)
      sqlContext.baseRelationToDataFrame(csvRelation)
    }
  }

  implicit class CsvSchemaRDD(dataFrame: DataFrame) {
    def saveAsCsvFile(path: String): Unit = {
      // TODO(hossein): For nested types, we may want to perform special work
      val header = dataFrame.columns.map(c => s""""$c"""").mkString(",")
      val strRDD = dataFrame.rdd.mapPartitions { iter =>
        new Iterator[String] {
          var firstRow: Boolean = true

          override def hasNext = iter.hasNext

          override def next: String = {
            if (firstRow) {
              firstRow = false
              header + "\n" + iter.next.mkString(",")
            } else {
              iter.next.mkString(",")
            }
          }
        }
      }
      strRDD.saveAsTextFile(path)
    }
  }
}
