<#macro fullcalendar id,events,width=900>
	<script type='text/javascript'>
		$(document).ready(function() {
			$('#${id}Calendar').fullCalendar({
				header: {
					left: 'prev,next today',
					center: 'title',
					right: 'month,agendaWeek,agendaDay'
				},
				editable: true,
				events: ${events},
				eventClick: function(event) {
					//window.open(event.url, 'gcalevent', 'width=700,height=600');
					if(event.opentype == "showDialog"){
						showDialog(event.url,event.showform,even.showtopic,event.showwidth,event.showheight);
					}else if(event.opentype=='openFullScreen'){
						openFullScreen(event.url);
					}
					return false;
				}
				
			});
			
			/**
				 * 显示增加对话框
				 * @method showDialog
				 * @param {String} url
				 * @private
				 */
				function showDialog222(url,formId,title,width,height) {
					dlg =	new CUI.Dialog({
						title: "${getHtmlText('" + title + "')}",
						url :url,
						formId: formId,
						modal:true,
						width:width || 650,
						height:height || 500,
						buttons:[{	name:"${getHtmlText('common.button.save')}",
									handler:function(){CUI('#'+formId).submit();}
								},
								{	name:"${getHtmlText('common.button.cancel')}",
									handler:function(){this.close()}
								}]
					});
					dlg.show();
					alert(4);
				}
	});
	</script>
	<style type='text/css'>
		body {
			text-align: center;
			font-size: 14px;
			font-family: "Lucida Grande",Helvetica,Arial,Verdana,sans-serif;
			}
	
		#${id}Calendar {
			width: ${width}px;
			margin: 0 auto;
			}
	</style>
	<div id='${id}Calendar'></div>
</#macro>