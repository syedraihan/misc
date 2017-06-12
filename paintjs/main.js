const Modes = {
    LINE: 'Line',
    CIRCLE: 'Circle',
    ELLIPSE: 'Ellipse',
    RECTANGLE: 'Rectangle',
    POLYGON: 'Polygon',
    POLYLINES: 'Polylines'
}

const Colors = {
    BLACK: 'Black',
    RED: 'Red',
    GREEN: 'Green',
    BLUE: 'Blue'
}

const Commands = {
    CLEAR: 'Clear',
    UNDO: 'Undo',
}

var _ctx;                       // Canvas 2D context 
var _objects = [];              // all 2D object drawn so far

var _inProgress = false;        // Is a drawing in progress?
var _mode = Modes.LINE;         // What is being drawn?
var _color = Colors.BLACK;      // In what color?
var _points = [];               // What are the points?

$(function() {
    // Initialzie the Canvas
    var canvas = document.getElementById('canvas');
    canvas.width = screen.width - 20;
    canvas.height = screen.height - 250;
    _ctx = canvas.getContext('2d');

    // Start of a new object (except for polygons)
    canvas.addEventListener('mousedown', function(evt) {
        var mousePos = getMousePos(canvas, evt);

        if (!_inProgress) { 
            // start a new object
            _points = [];
            _points.push(mousePos);
            _inProgress = true;
        }
    }, false);

    // Finish of a object drawing (except for polygons)
    canvas.addEventListener('mouseup', function(evt) {
        mousePos = getMousePos(canvas, evt);
        _points.push(mousePos); 

        switch (_mode) {
            case Modes.POLYGON:
            case Modes.POLYLINES:
                // for polygon and ploylines, 
                // click does not finish the object
                break;

            default:
                // for all other, finish object 
                finishObject();
        }
    }, false);

    // Finish of any object drawing (including polygons)
    canvas.addEventListener('dblclick', function(){ 
        finishObject();
    }, false);

    // Rubberbanding logic
    canvas.addEventListener('mousemove', function(evt) {
        mousePos = getMousePos(canvas, evt);
        $('#status').text('x=' + mousePos.x + ' y=' + mousePos.y);

        if (_inProgress) {
            redrawAll();
            drawObject(_mode, _points.concat([mousePos]));
        }
    }, false);

    // Handle click events of the top bar buttons
    $(".btn-group > .btn").click(function(){
        var cmd = $(this).text();
        doCommand(cmd);
    });

    // Needed for proper functioning of the toggle buttons
    $(".mcq > .btn").click(function(){
        $(this).addClass("active").siblings().removeClass("active");
    });
});

function doCommand(cmd)
{
    switch (cmd) {
        case Modes.LINE:
        case Modes.CIRCLE:
        case Modes.ELLIPSE:
        case Modes.RECTANGLE:
        case Modes.POLYGON:
        case Modes.POLYLINES:
            _mode = cmd;
            break;

        case Colors.RED:    
        case Colors.GREEN:  
        case Colors.BLUE:   
        case Colors.BLACK:  
            setColor(cmd);
            _color = cmd;
            break;

        case Commands.CLEAR: 
            _objects = []; 
            redrawAll();
            break;
        case Commands.UNDO: 
            _objects.splice(-1, 1);
            redrawAll();
    }
}

function setColor(color)
{
    switch (color) {
        case Colors.RED:    _ctx.strokeStyle = "#FF0000"; break;
        case Colors.GREEN:  _ctx.strokeStyle = "#00FF00"; break;
        case Colors.BLUE:   _ctx.strokeStyle = "#0000FF"; break;
        case Colors.BLACK:  _ctx.strokeStyle = "#000000"; break;
    }    
}

function finishObject()
{
    _inProgress = false;

    // remember the object definition 
    // so we redraw later (needed to rubberbanding) 
    _objects.push ({
        mode: _mode,
        points: _points,
        color: _color
    })
}

function redrawAll()
{
    _ctx.clearRect(0, 0, canvas.width, canvas.height);

    for (var i = 0, len = _objects.length; i < len; i++) {
        var obj = _objects[i];
        setColor(obj.color);
        drawObject(obj.mode, obj.points);
    }

    setColor(_color);
}

function drawObject(mode, points)
{
    // Except for polygons all objects just have 
    // two points defined.
    var startPos = points[0], endPos = points[1];

    switch(mode) {
        case Modes.LINE:
            drawLine(_ctx, startPos.x, startPos.y, endPos.x, endPos.y);
            break;

        case Modes.RECTANGLE:
            drawRectangle(_ctx, startPos.x, startPos.y, endPos.x, endPos.y);
            break;

        case Modes.CIRCLE:
        case Modes.ELLIPSE:
            // Calculate center (xc, yc)
            var xc = startPos.x, 
                yc = startPos.y;
            
            // Find the height/width of the bounding rectangle,
            // so that we can calculate the radious
            var a = Math.abs(startPos.x - endPos.x), 
                b = Math.abs(startPos.y - endPos.y);
            
            if (mode == Modes.CIRCLE) {
                // a^2 + b^2 = r^2
                var r = Math.sqrt(a * a + b * b);
                drawCircle(_ctx, xc, yc, r);
            } else 
                // rx = a, ry = b                
                drawEllipse(_ctx, xc, yc, a, b);
            break;

        case Modes.POLYGON:
        case Modes.POLYLINES:
            // Polygon and Polylines are the same thing
            // just let the drawing routine 
            // weather we want a closed one or an open one
            drawPolygon(_ctx, points, mode == Modes.POLYGON)
    }
}

function getMousePos(canvas, evt) 
{
    var rect = canvas.getBoundingClientRect();
    return {
        x: evt.clientX - rect.left,
        y: evt.clientY - rect.top
    };
}