> **_NOTE:_**  Please update the documentation and the diagrams as you make
> changes to the graphical editor. To edit diagrams, edit .odg files on
> [OwnCloud](https://share.greendelta.com/index.php/apps/files/?dir=/CodeDocumentation/olca-app-graphical)

# Graphical Editor Documentation

## MVC and Eclipse GEF

The Graphical Editor of OpenLCA is built with the [classical Eclipse Graphical
Editor Framework (GEF)](https://github.com/eclipse/gef-classic).
GEF provides tools to create a graphical editor within the [Model View
Controller pattern](
https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93controller).
The program logic is separated into three types of modules having three
different responsibilities. Models contain data to be displayed. Views are the
representation of a model's data. Controllers take care of the logic behind
actions made by the user (see schema below by Евгений Мирошниченко).

<p align="center">
  <img src=https://share.greendelta.com/index.php/s/ADFCZBEX5PW64g2/download
       width="700"/>
</p>

To be acquainted with GEF and the MVC structure, good starting points are these
documents ([1](
https://wiki.eclipse.org/GEF_Description#getContentPane.28.29_method) & [2](
https://wiki.eclipse.org/GEF_Description2)) written by Régis Lemaigre, [Create
an Eclipse-based application using the Graphical Editing Framework](
https://web.archive.org/web/20070216144311/http://www-128.ibm.com/developerworks/opensource/library/os-gef/)
by Randy Hudson and the [IBM RedBook](
https://www.redbooks.ibm.com/redbooks/pdfs/sg246302.pdf).

## OpenLCA graphical Editor.

### Models

The parent of all models in the `PRINTABLE_LAYER` is the `Graph`. It is the
white (or any other color by the way) canvas that contains `Nodes`.  A `Node`
is the graphical representation of a process,  a product system, a library or a
result. A `Node` can be _maximized_ and thus have `IOPanes` with `ExchangeItems`
or it can be _minimized_. An `IOPane` lists _input_ or _output_ flows of a
`Node`. Those flows are represented by `ExchangeItems`.

Components of the `Graph` are connected with `Links`. A `Link` is part of the
`CONNECTION_LAYER`. It can connect a _minimized_ `Node` (or an `ExchangeItem`)
to another _minimized_ `Node` (or an `ExchangeItem`).

Finally `GraphConfig` gathers parameters chosen by the user within the Graph
Settings menu.

Model entities inherit from abstract classes :
 - `GraphElement`: notifies `EditParts` with a `PropertyChangeSupport`.
 - `GraphComponent`: provides features necessary for all model components:
   - management of size and location,
   - support for adding and removing children,
   - methods for connections with other `GraphComponents`.
 - `MinMaxGraphComponents`: supplies with methods for `GraphComponents`
that can be _minimized_ (remove all children and shrink the dimensions) or
_maximized_ (add necessary children and adapt the size).

<p align="center">
  <img src="https://share.greendelta.com/index.php/s/YWuP8aLYXCrwcLd/download"
       width="400"/>
</p>

### EditParts and EditPolicy

`EditParts` are the controllers in the MVC structure. Each model object has an
`EditPart` that controls every edition of the model and transformation of the
figures. `EditParts` creation is managed by the `GraphEditPartFactory` which
convert a model to its corresponding `EditPart`.

The graphical editor `EditParts` classes inherit from:
 - `AbstractConnectionEditPart`: manages connections from a source to a target
`GraphComponent`,
 - `AbstractGraphicalEditPart`: ties model to their figures,
 - `AbstractComponentEditPart`: provides tools to listen to models' changes,
 - `AbstractVertexEditPart`: supplies with connection management methods.

<p align="center">
  <img src=https://share.greendelta.com/index.php/s/oPObw8TzvuutMeR/download
       width="400"/>
</p>

To edit a model, one should create a `Request`. This should be forwarded to an
edit part using the `getCommand` method. The `EditPart` then takes care of
dispatching the requests to the corresponding `EditPolicy`. The `EditPolicy`
creates and returns the potential command (or set of commands with a
`CompoundCommand`). An `EditPolicy` object can _create_ command, _delegate_ and
_forward_ requests when an editing request requires multiple edit parts edition
and _manage_ connection feedback (e.g. when deleting a `Node`).

### Figures

Edit parts create figures. When a modification is made to the model, the model
notifies the `EditPart` with a listener and the edit part refreshes the
corresponding figure, connections or children within the `propertyChange`
method.

Children's figures of an edit part are added to the content pane of its figure.
This means that an `EditPartA` child's figure is not necessary a child of
`FigureA` and can be a child of `FigureA.contentPane`.

The diagram below shows the structure of the graphical editor with on the left
side models, followed by controllers and views.

<p align="enter">
  <img src=https://share.greendelta.com/index.php/s/bZ2uaxH6e9u4BVJ/download />
</p>

## Actions

GEF also implement `Actions` that are derived from JFace Actions. A GEF `Action`
proposes specific features such as being selection-specific or
`CommandStack`-specific. As every change of the model and the view should go
through the edit part, the execution and the ability to be executed is provided
by the `EditPart`.

As an example, the course of operation for the execution of _Minimize All_ is as
follows:
 - For each targeted `MinMaxComponent`:
   - a `Request(REQ_MIN)` is created and then forwarded to the `EditPart`,
   - the `EditPart` then forwards the request to its corresponding `EditPolicy`,
   - the `EditPolicy` creates (or forwards the request) a `Command`,
   - the command is forwarded back to the `MinMaxAllAction`.
 - All the commands are packed into a `CompoundCommand` that can be executed if
all the commands are executable,
 - The action is run by the user when clicking on the menu.
