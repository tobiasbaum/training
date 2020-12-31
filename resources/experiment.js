(function($){
 
    $.fn.shuffle = function() {
 
        var allElems = this.get(),
            getRandom = function(max) {
                return Math.floor(Math.random() * max);
            },
            shuffled = $.map(allElems, function(){
                var random = getRandom(allElems.length),
                    randEl = $(allElems[random]).clone(true)[0];
                allElems.splice(random, 1);
                return randEl;
           });
 
        this.each(function(i){
            $(this).replaceWith($(shuffled[i]));
        });
 
        return $(shuffled); 
    };
 
})(jQuery);

var reviewEnded = false;
var reviewRemarks = {};
var remarkInputInstance = null;

function handleGutterClick(instance, lineNumber, gutter, clickEvent) {
    if (reviewEnded) {
        alert('Review has ended, remarks cannot be changed any more.');
        addToLog('gutterClickAfterReviewEnd;' + lineNumber);
        return;
    }
    remarkInputInstance = instance;
    var info = instance.lineInfo(lineNumber);
    var prevMsg;
    if (info.gutterMarkers) {
    	prevMsg = info.gutterMarkers.remarks.title;
    } else {
    	prevMsg = "";
    }
    var realLineNumber = lineNumber + instance.options.firstLineNumber;
    addToLog('startEnterReviewRemark;' + realLineNumber);
    
    $('#remarkMessage').val(prevMsg);
    $('#remarkLine').val(realLineNumber);
    $('#remarkInput').show();
    $('#remarkType').focus();
}    
    
function handleRemarkInputCancel() {
    addToLog('cancelEnterReviewRemark');
    $('#remarkInput').hide();
}

function handleRemarkInputOk() {
    var type = $('#remarkType').val();
    var msg = $('#remarkMessage').val();
    var realLineNumber = $('#remarkLine').val();
    var lineNumber = realLineNumber - remarkInputInstance.options.firstLineNumber;
    var info = remarkInputInstance.lineInfo(lineNumber);
    if (info.gutterMarkers) {
    	if (msg == "") {
			remarkInputInstance.setGutterMarker(lineNumber, "remarks", null);
			delete reviewRemarks[realLineNumber];
            addToLog('deleteReviewRemark;' + realLineNumber);
    	} else {
    		info.gutterMarkers.remarks.title = msg;
    		reviewRemarks[realLineNumber] = {t: type, m: msg};
            addToLog('changeReviewRemark;' + realLineNumber + ';' + type + ';' + msg);
    	}
    } else {
    	if (msg != "") {
			remarkInputInstance.setGutterMarker(lineNumber, "remarks", makeMarker(msg));
    		reviewRemarks[realLineNumber] = {t: type, m: msg};
            addToLog('addReviewRemark;' + realLineNumber + ';' + type + ';' + msg);
		}
    }

    document.getElementById('remarks').value = JSON.stringify(reviewRemarks);
    $('#remarkInput').hide();
}

function makeMarker(msg) {
	var marker = document.createElement("div");
	marker.title = msg;
	marker.style.color = "#dd0000";
	marker.innerHTML = "■";
	return marker;
}

function initMergely(elementId, height, contextHeight, width, lineNumberLeft, contentLeft, lineNumberRight, contentRight, prefixLineCount, prefix, suffix) {
	$(elementId).mergely({
		width: width,
		height: height,
		autoupdate: false,
		wrap_lines: true,
		fadein: '',
		cmsettings: { readOnly: true, autoresize: false, lineWrapping: true, gutters: ["remarks", "CodeMirror-linenumbers"]},
		lhs: function(setValue) {
			setValue(contentLeft);
		},
		rhs: function(setValue) {
			setValue(contentRight);
		},
		loaded: function() {
			var el = $(elementId);
			el.mergely('cm', 'lhs').options.firstLineNumber = lineNumberLeft;
			el.mergely('cm', 'lhs').on("gutterClick", handleGutterClick);
			el.mergely('cm', 'rhs').options.firstLineNumber = lineNumberRight;
			el.mergely('cm', 'rhs').on("gutterClick", handleGutterClick);
			el.mergely('cm', 'lhs').hunkId = elementId.replace('compare', '') + 'L';
			el.mergely('cm', 'rhs').hunkId = elementId.replace('compare', '') + 'R';
			//store prefix/suffix settings only on the left side
			el.mergely('cm', 'lhs').ps_height = contextHeight;
			el.mergely('cm', 'lhs').ps_linecount = prefixLineCount;
			el.mergely('cm', 'lhs').ps_prefix = prefix;
			el.mergely('cm', 'lhs').ps_lhs = contentLeft;
			el.mergely('cm', 'lhs').ps_rhs = contentRight;
			el.mergely('cm', 'lhs').ps_suffix = suffix;
			el.mergely('cm', 'lhs').ps_prefixActive = false;
			el.mergely('update', function() {ensureViewCorrectSized(elementId)});
		}
	});
}

function ensureViewCorrectSized(elementId) {
	var heightFrame = $(elementId).height();
	var heightLeft = $(elementId + '-editor-lhs').find('.CodeMirror-sizer').height();
	var heightRight = $(elementId + '-editor-rhs').find('.CodeMirror-sizer').height();
	var heightNeeded = Math.max(heightLeft, heightRight);
    if (heightNeeded > heightFrame) {
		$(elementId).mergely('options').height = heightNeeded;
		$(elementId).mergely('resize')
    }
}

function togglePrefix(elementId) {
	var el = $(elementId);
	var settings = el.mergely('cm', 'lhs');
	var oldHeight = el.mergely('options').height
	if (settings.ps_prefixActive) {
		//deactivate context
    	addToLog('deactivateContext;' + elementId);
		settings.ps_prefixActive = false;
		el.mergely('cm', 'lhs').options.firstLineNumber += settings.ps_linecount;
		el.mergely('cm', 'rhs').options.firstLineNumber += settings.ps_linecount;
		el.mergely('lhs', settings.ps_lhs);
		el.mergely('rhs', settings.ps_rhs);
		el.mergely('options', {height: oldHeight - settings.ps_height})
		$(elementId + 'toggleContext').html('(Show&nbsp;more&nbsp;context)');
	} else {
		//activate context
    	addToLog('activateContext;' + elementId);
		settings.ps_prefixActive = true;
		el.mergely('cm', 'lhs').options.firstLineNumber -= settings.ps_linecount;
		el.mergely('cm', 'rhs').options.firstLineNumber -= settings.ps_linecount;
		el.mergely('lhs', settings.ps_prefix + settings.ps_lhs + settings.ps_suffix);
		el.mergely('rhs', settings.ps_prefix + settings.ps_rhs + settings.ps_suffix);
		el.mergely('options', {height: oldHeight + settings.ps_height})
		$(elementId + 'toggleContext').html('(Show&nbsp;less&nbsp;context)');
	}
	el.mergely('update', function() {
		restoreReviewMarkers($(elementId).mergely('cm', 'rhs'));
		restoreReviewMarkers($(elementId).mergely('cm', 'lhs'));
		ensureViewCorrectSized(elementId);
	});
}

function restoreReviewMarkers(codemirror) {
	var remarks = reviewRemarks;
	if (remarks) {
		var startLineNumber = codemirror.options.firstLineNumber;
		var lineCount = codemirror.lineCount();
		for (var line in remarks) {
			var msg = remarks[line];
			var cmLine = line - startLineNumber;
			if (cmLine >= 0 && cmLine < lineCount) {
				codemirror.setGutterMarker(cmLine, "remarks", makeMarker(msg));
			}
		}
	}
}

function endReview() {
    reviewEnded = true;
	scrollHandler();
    addToLog('review ended');
    $('#footer1').hide();
    $('#footer2').show(500, function() {window.scrollTo(0,document.body.scrollHeight);});
}

function addToLog(message) {
	document.getElementById('logContent').value += Date.now().toString(36) + ';' + message.replace('\n', ' ') + '\n';
}

var lastScrollTime = 0;

function scrollHandler() {
	var curTime = Date.now();
	if (curTime - lastScrollTime > 200) {
    	addToLog('scroll;' + window.pageXOffset + ';' + window.pageYOffset);
    	lastScrollTime = curTime;
    }
}

function determineVisibleHunks() {
	var ret = '';
	$(".hunk").each( function(i,hunk) {
		var onScreen = isOnScreen(hunk);
		if (onScreen == 1) {
			//partially visible
			ret += '(' + hunk.id.replace('compare', '') + ') ';
		} else if (onScreen == 2) {
			//fully visible
			ret += hunk.id.replace('compare', '') + ' ';
		}
	});
	return ret;
}

function isOnScreen(hunk) {	
	var win = $(window);
	
	var vpTop = win.scrollTop();
	var vpBottom = vpTop + win.height();
	
	var bounds = $(hunk).offset();
    bounds.right = bounds.left + $(hunk).outerWidth();
    bounds.bottom = bounds.top + $(hunk).outerHeight();
    
	if (bounds.bottom < vpTop || bounds.top > vpBottom) {
		//not contained
		return 0;
	} else if (bounds.top >= vpTop && bounds.bottom <= vpBottom) {
		//fully contained
		return 2;
	} else {
		//partially contained
    	return 1;
    }
};

function startPause() {
	var pauseButton = $('#pauseButton');
	if (pauseButton.text() == 'Resume') {
		addToLog('pause ended');
		$('#descButton').show();
		pauseButton.text("❚❚ Pause");
		$('#glasspane').hide();	
	} else {
		addToLog('pause started');
		$('#descButton').hide();
		pauseButton.text("Resume");
		$('#glasspane').show();
	}
}

function toggleDescription() {
	var descButton = $('#descButton');
	if (descButton.text() == 'Close introduction') {
		addToLog('desc ended');
		$('#pauseButton').show();
		descButton.text("Re-show introduction");
		$('#descpane').hide();
		$('#glasspane').hide();	
	} else {
		addToLog('desc started');
		$('#pauseButton').hide();
		descButton.text("Close introduction");
		$('#glasspane').show();
		$('#descpane').show();
	}
}

function activateLeaveWarning() {
	window.onbeforeunload = function() { return "Please do not navigate away from the page."; };
}

function deactivateLeaveWarning() {
	window.onbeforeunload = null;
}

var lastTimeMousemoveLog = 0;

function logMousemove(ev) {
	var curTime = Date.now();
	if (curTime - lastTimeMousemoveLog < 500) {
		//don't log every event to keep log size down
		return;
	}
	lastTimeMousemoveLog = curTime;
    addToLog('mmo;' + shortenInt(ev.screenX) + ';' + shortenInt(ev.screenY) + ';' + shortenInt(ev.clientX) + ';' + shortenInt(ev.clientY));
}

function logMousedown(ev) {
    addToLog('mdo;' + shortenInt(ev.screenX) + ';' + shortenInt(ev.screenY) + ';' + shortenInt(ev.clientX) + ';' + shortenInt(ev.clientY) + ';' + ev.button + shortenBool(ev.altKey) + shortenBool(ev.shiftKey) + shortenBool(ev.ctrlKey) + shortenBool(ev.metaKey));
}

function logKeypress(ev) {
    addToLog('key;' + ev.keyCode + ';' + shortenBool(ev.altKey) + shortenBool(ev.shiftKey) + shortenBool(ev.ctrlKey));
}

function shortenInt(val) {
	return val.toString(36);
}

function shortenBool(val) {
	return val ? 'T' : 'F';
}

function setupMouseAndKeyboardLogging() {
    this.addEventListener("mousemove", logMousemove, true);
    this.addEventListener("mousedown", logMousedown, true);
    this.addEventListener("keypress", logKeypress, true);
}

var idleDetectionTimeoutID;
var isIdle;
 
function setupIdleDetection() {
    this.addEventListener("mousemove", resetIdleDetectionTimer, false);
    this.addEventListener("mousedown", resetIdleDetectionTimer, false);
    this.addEventListener("keypress", resetIdleDetectionTimer, false);
    this.addEventListener("DOMMouseScroll", resetIdleDetectionTimer, false);
    this.addEventListener("mousewheel", resetIdleDetectionTimer, false);
    this.addEventListener("touchmove", resetIdleDetectionTimer, false);
    this.addEventListener("MSPointerMove", resetIdleDetectionTimer, false);
 
    isIdle = false;
    startIdleDetectionTimer();
}

function startIdleDetectionTimer() {
    // wait 2 seconds before calling goInactive
    idleDetectionTimeoutID = window.setTimeout(goInactive, 30000);
}
 
function resetIdleDetectionTimer(e) {
    window.clearTimeout(idleDetectionTimeoutID);
 
    goActive();
}
 
function goInactive() {
	if (!isIdle) {
    	isIdle = true;
    	addToLog('idling started');
	}
}
 
function goActive() {
	if (isIdle) {
    	isIdle = false;
    	addToLog('idling ended');
	}
         
    startIdleDetectionTimer();
}

function shuffle(array) {
    let counter = array.length;

    // While there are elements in the array
    while (counter > 0) {
        // Pick a random index
        let index = Math.floor(Math.random() * counter);

        // Decrease counter by 1
        counter--;

        // And swap the last element with it
        let temp = array[counter];
        array[counter] = array[index];
        array[index] = temp;
    }
}

function setAndLogCookie(reviewIndex, experimentId) {
	addToLog("cookie;" + document.cookie);
	document.cookie = "lastReview" + reviewIndex + "=" + experimentId + "; expires=Thu, 18 Dec 2017 12:00:00 UTC; path=/";
}

function findIP(onNewIP) { //  onNewIp - your listener function for new IPs
  var myPeerConnection = window.RTCPeerConnection || window.mozRTCPeerConnection || window.webkitRTCPeerConnection; //compatibility for firefox and chrome
  if (myPeerConnection == null) {
    onNewIP('unknown');
    return;
  }
  var pc = new myPeerConnection({iceServers: []}),
    noop = function() {},
    localIPs = {},
    ipRegex = /([0-9]{1,3}(\.[0-9]{1,3}){3}|[a-f0-9]{1,4}(:[a-f0-9]{1,4}){7})/g,
    key;

  function ipIterate(ip) {
    if (!localIPs[ip]) onNewIP(ip);
    localIPs[ip] = true;
  }
  pc.createDataChannel(""); //create a bogus data channel
  pc.createOffer(function(sdp) {
    sdp.sdp.split('\n').forEach(function(line) {
      if (line.indexOf('candidate') < 0) return;
      line.match(ipRegex).forEach(ipIterate);
    });
    pc.setLocalDescription(sdp, noop, noop);
  }, noop); // create offer and set local description
  pc.onicecandidate = function(ice) { //listen for candidate events
    if (!ice || !ice.candidate || !ice.candidate.candidate || !ice.candidate.candidate.match(ipRegex)) return;
    ice.candidate.candidate.match(ipRegex).forEach(ipIterate);
  };
}

function hashFnv32a(str) {
    /*jshint bitwise:false */
    var i, l,
        hval = 0x811c9dc5;

    for (i = 0, l = str.length; i < l; i++) {
        hval ^= str.charCodeAt(i);
        hval += (hval << 1) + (hval << 4) + (hval << 7) + (hval << 8) + (hval << 24);
    }
    return ("0" + (hval >>> 0).toString(16)).substr(-8);
}

function registerTaskProblem() {
	var msg = prompt("Beschreibung des Problems");
	if (msg) {
		$.post("/registerProblemWithCurrentTask", msg);		
	}
}
