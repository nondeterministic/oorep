package org.multics.baueran.frep.frontend.base

import scalatags.JsDom.TypedTag
import scalatags.JsDom.all.{li, _}
import scalatags.JsDom.tags2.nav

object NavBar {
  def apply(): TypedTag[org.scalajs.dom.html.Element] = {
    nav(cls:="navbar py-0 fixed-top navbar-expand-sm bg-dark navbar-dark",
      button(cls:="navbar-toggler", `type`:="button", data.toggle:="collapse", data.target:="#navbarToggler",
        span(cls:="navbar-toggler-icon")),
      a(cls:="navbar-brand", href:="#",
        img(src:="logo_small.png")
      ),
      div(cls:="collapse navbar-collapse", id:="navbarToggler",
        div(cls:="ml-auto",
          ul(cls:="navbar-nav",
            li(cls:="navbar-item active", a(cls:="nav-link", href:="", onclick:={ () => println("pressed1") })("About")),
            li(cls:="navbar-item active", a(cls:="nav-link", href:="", onclick:={ () => println("pressed1") })("Features")),
            li(cls:="navbar-item active", a(cls:="nav-link", href:="", onclick:={ () => println("pressed2") })("Pricing")),
            li(cls:="navbar-item active", a(cls:="nav-link", href:="", onclick:={ () => println("pressed2") })("FAQ")),
            li(cls:="navbar-item active", a(cls:="nav-link", href:="", onclick:={ () => println("pressed2") })("Contact")),
            li(cls:="navbar-item active", style:="background-color: SeaGreen; margin-right: 5px; margin-left:10px;",
              a(cls:="nav-link", href:="", onclick:={ () => println("pressed1") })("Login")),
            li(cls:="navbar-item active", style:="background-color: DeepPink;",
              a(cls:="nav-link", href:="", onclick:={ () => println("pressed2") })("Register"))
          )
        )
      )
    )
  }
}

//<nav class="navbar navbar-expand-lg navbar-light bg-light">
//  <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarTogglerDemo03" aria-controls="navbarTogglerDemo03" aria-expanded="false" aria-label="Toggle navigation">
//    <span class="navbar-toggler-icon"></span>
//  </button>
//  <a class="navbar-brand" href="#">Navbar</a>
//
//  <div class="collapse navbar-collapse" id="navbarTogglerDemo03">
//    <ul class="navbar-nav mr-auto mt-2 mt-lg-0">
//      <li class="nav-item active">
//        <a class="nav-link" href="#">Home <span class="sr-only">(current)</span></a>
//      </li>
//      <li class="nav-item">
//        <a class="nav-link" href="#">Link</a>
//      </li>
//      <li class="nav-item">
//        <a class="nav-link disabled" href="#">Disabled</a>
//      </li>
//    </ul>
//    <form class="form-inline my-2 my-lg-0">
//      <input class="form-control mr-sm-2" type="search" placeholder="Search" aria-label="Search">
//      <button class="btn btn-outline-success my-2 my-sm-0" type="submit">Search</button>
//    </form>
//  </div>
//</nav>