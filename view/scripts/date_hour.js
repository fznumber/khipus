function show5(hours,minutes,seconds,day,month,year){
//function show5(Digital,hours){
    if (!document.layers&&!document.all&&!document.getElementById)
        return


//    if( arguments.length == 1)
    if( arguments.length == 1)
    {
        var fecha = document.getElementById("markRegister:marDate").value
        var hora = document.getElementById("markRegister:marTime").value
        //yyyy-MM-dd
        var dateString = fecha+" "+hora;
        var reggie = /(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})/;
        var dateArray = reggie.exec(dateString);
        var Digital = new Date(
            (+dateArray[1]),
            (+dateArray[2])-1, // Careful, month starts at 0!
            (+dateArray[3]),
            (+dateArray[4]),
            (+dateArray[5]),
            (+dateArray[6])
        )
        var hours=Digital.getHours()
        var minutes=Digital.getMinutes()
        var seconds=Digital.getSeconds()
        var day = Digital.getDate()
        var month = Digital.getMonth() + 1
        var year = Digital.getFullYear()
    }else
    {
        var Digital = new Date( year,
                                month-1, // Careful, month starts at 0!
                                day,
                                hours,
                                minutes,
                                seconds
                                )
    }

        Digital.setSeconds(Digital.getSeconds()+1)
        var hours=Digital.getHours()
        var minutes=Digital.getMinutes()
        var seconds=Digital.getSeconds()
        var day = Digital.getDate()
        var month = Digital.getMonth() + 1
        var year = Digital.getFullYear()


    /*var dn="PM"
    if (hours<12)
        dn="AM"
    if (hours>12)
        hours=hours-12
    if (hours==0)
        hours=12
*/
    if (minutes<=9)
        minutes="0"+minutes
    if (seconds<=9)
        seconds="0"+seconds

    fecha_servidor="<font face='Arial' ><b><font size='3'>Fecha del Servidor: </font>"+day+"/"+month+"/"+year+"</b></font>"
    hora_servidor="<font face='Arial' ><b><font size='3'></font>"+hours+":"+minutes+":"+seconds+"</b></font>"

    if (document.layers){
        document.layers.time_server.document.write(hora_servidor)
        document.layers.date_server.document.write(fecha_servidor)
        document.layers.liveclock.document.close()
    }
    else if (document.all)
         {
             time_server.innerHTML=hora_servidor
             date_server.innerHTML=fecha_servidor
         }
    else if (document.getElementById)
         {
             document.getElementById("time_server").innerHTML=hora_servidor
             document.getElementById("date_server").innerHTML=fecha_servidor
         }

    setTimeout(function(){show5(hours,minutes,seconds,day,month,year)},1000)

}
window.onload=show5