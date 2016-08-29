import groovy.sql.Sql
class GroovySqlExample1{
  static void main(String[] args) {
    def sql = Sql.newInstance("jdbc:mysql://localhost:3306/bdopenehr", "root", "", "com.mysql.jdbc.Driver")
    sql.eachRow("select * from ehr"){ row ->
       println row
    }
     sql.eachRow("select d.*, e.uid as ehr_uid from document d, ehr e where d.ehr_id = e.id"){ row ->
       println row
    }
  }
}