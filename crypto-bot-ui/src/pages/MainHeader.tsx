import {Link} from "wouter";
import '../css/HeaderLinkBox.css';
import '../css/CuteLink.css';

const MainHeader = () => {
    return (
        <div className='links-box'>
            <div className='links-box-item'><Link to={"/analyzer"} className='cute-link'>Analyzers List</Link></div>
            <div className='links-box-item'><Link to={"/manager"} className='cute-link'>Managers List</Link></div>
        </div>
    )
}

export default MainHeader;
