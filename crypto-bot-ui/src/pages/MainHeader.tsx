import {Link} from "wouter";
import '../css/HeaderLinkBox.css';
import '../css/CuteLink.css';

const MainHeader = () => {
    let userData = localStorage.getItem('auth.token');

    if (!userData) {
        return (
            <div className='links-box'>
                <div className='links-box-item'><Link to={"/login"} className='cute-link'>Login</Link></div>
                <div className='links-box-item'><Link to={"/signup"} className='cute-link'>Sign Up</Link></div>
            </div>
        )
    }

    const role = localStorage.getItem('auth.role')

    return (
        <div className='links-box'>
            <div className='links-box-item'><Link to={"/analyzer"} className='cute-link'>Analyzers List</Link></div>
            <div className='links-box-item'><Link to={"/top-analyzers"} className='cute-link'>Public Top Analyzers</Link></div>
            <div className='links-box-item'><Link to={"/account"} className='cute-link'>Account</Link></div>
            <div className='links-box-item'><Link to={"/manager"} className='cute-link'>Managers List</Link></div>
            {
                role === 'ADMIN' &&
                <div className='links-box-item'>
                    <Link to={"/symbols"} className='cute-link'>Symbols</Link>
                </div>
            }
        </div>
    )
}

export default MainHeader;
