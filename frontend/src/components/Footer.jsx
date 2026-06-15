function Footer() {
  return (
    <footer className="bg-light border-top py-3 mt-auto">
      <div className="container text-center text-muted">
        <small>
          &copy; {new Date().getFullYear()} Employee Management System. All rights reserved.
        </small>
      </div>
    </footer>
  )
}

export default Footer
