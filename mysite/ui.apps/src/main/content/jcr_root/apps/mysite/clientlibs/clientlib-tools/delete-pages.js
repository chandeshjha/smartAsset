$(document).ready(function () {

  $("#btnDeleteSubmit").click(function (event) {
    let jsonValue = $("#publishPagesJSON").val();
    event.preventDefault();

    callServlet(jsonValue);
  });

  function callServlet(jsonValue) {
      console.log("JSON Value" + jsonValue);
    $.ajax({
      url: "/content/mysite/us/_jcr_content/delete.delete-pages.json",
      method: "POST",
      headers: { 'myParam' : jsonValue },
      processData: false,
      cache: false,
      contentType: 'application/json',
      success: function (data,textStatus,jqXHR) {
        console.log(data + "text status" + textStatus + "     " +jqXHR);
      },
      error: function (xhr) {
        console.log(xhr)
      }
    });
  }

});