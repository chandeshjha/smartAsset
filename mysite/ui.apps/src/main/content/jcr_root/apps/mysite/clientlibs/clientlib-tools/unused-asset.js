$(document).ready(function () {

  $("#btnAssetFetch").click(function (event) {
    let assetFolderPath = $("#asset-path").val();
    if (assetFolderPath.length > 1) {
      //stop submit the form, we will post it manually.
      event.preventDefault();
      // disabled the submit button
      $("#btnAssetFetch").prop("disabled", true);
      callServlet(assetFolderPath);
    }
  });

  function callServlet(assetFolderPath) {
    $.ajax({
      type: "GET",
      url: "/bin/assetref",
      data: "path=" + assetFolderPath,
      processData: false,
      contentType: 'text/plain',
      cache: false,
      success: function (data1) {
        createtable("asset-table", data1);
      },
      error: function (e) {
        console.log({ e });
      }
    });
  }
  function createtable(tableId, data) {
    let assetTable = document.getElementById(tableId);
    let table = new Coral.Table();
    let tableBody = new Coral.Table.Body();
    createTableHeader(tableBody);
    createRow(tableBody, data);
    table.appendChild(tableBody);
    assetTable.appendChild(table);
  }

  function createTableHeader(tableBody) {
    let row = new Coral.Table.Row();
    row.appendChild(new Coral.Table.HeaderCell().set({
      content: {
        textContent: "Name"
      }
    }));
    row.appendChild(new Coral.Table.HeaderCell().set({
      content: {
        textContent: "Path"
      }
    }));

    tableBody.appendChild(row);

  }

  function createRow(tableBody, data) {
    for (const key in data) {
      let row = new Coral.Table.Row();
      row.appendChild(new Coral.Table.Cell().set({
        content: {
          textContent: key
        }
      }));
      row.appendChild(new Coral.Table.Cell().set({
        content: {
          textContent: data[key]
        }
      }));
      tableBody.appendChild(row);
    }
  }

});